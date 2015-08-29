/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.DateTimeConverters
import core.mongodb.DManFS
import dman.Lock.LockOpStatusTypes._
import dman.MetadataKeys._
import models.customer.CustomerId
import models.parties.UserId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.iteratee.Enumerator

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Represents a file to be up/down -loaded by a User.
 *
 * This is <i>NOT</i> a file in the sense of a java.util.File. But rather a wrapper around an InputStream with
 * quite a bit of extra Metadata information. The Metadata is mapped to the GridFS "<bucket>.files" collection, and
 * the InputStream is read from the "<bucket>.chunks" collection.
 */
case class File(
  id: Option[FileId] = None,
  filename: String,
  contentType: Option[String] = None,
  uploadDate: Option[DateTime] = None,
  size: Option[String] = None, // Same as the length field in GridFS...but as String to prevent data loss in JS clients
  stream: Option[FileStream] = None,
  // The following fields will be added to the GridFS Metadata in fs.files...
  metadata: FileMetadata) extends BaseFile {

  /**
   * Feeds the InputStream bytes into an Enumerator
   */
  def enumerate(implicit ec: ExecutionContext): Option[Enumerator[Array[Byte]]] = stream.map(s => Enumerator.fromStream(s))

  /**
   * Build up the necessary metadata for persisting in GridFS
   */
  def buildBSON: Metadata = {
    val builder = MongoDBObject.newBuilder
    id.foreach(builder += "_id" -> FileId.asObjId(_))
    FileMetadata.toBSON(metadata) ++ builder.result()
  }
}

object File extends DateTimeConverters with DManFS {

  val logger = LoggerFactory.getLogger(File.getClass)

  /**
   * Converter to map between a GridFSDBFile (from read operations) to a File
   *
   * @param gf GridFSDBFile
   * @return File
   */
  def fromGridFS(gf: GridFSDBFile): File = {
    val md = new MongoDBObject(gf.metaData)
    File(
      id = FileId.asMaybeId(gf._id),
      filename = gf.filename.getOrElse("no_name"),
      contentType = gf.contentType,
      uploadDate = Option(asDateTime(gf.uploadDate)),
      size = Option(gf.length.toString),
      stream = Option(gf.inputStream),
      metadata = FileMetadata.fromBSON(md)
    )
  }

  /**
   * Converter to map between a DBObject (from read operations) to a File.
   * This will typically be used when listing files in a GridFS <bucket>.files collection
   *
   * @param dbo DBObject
   * @return File
   */
  def fromBSON(dbo: DBObject): File = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    File(
      id = FileId.asMaybeId(mdbo._id),
      filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
      contentType = mdbo.getAs[String]("contentType"),
      uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
      size = mdbo.getAs[Long]("length").map(_.toString),
      stream = None,
      metadata = FileMetadata.fromBSON(md)
    )
  }

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param f File
   * @return Option[ObjectId]
   */
  def save(f: File): Option[FileId] = {
    Try {
      f.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = f.filename
        f.contentType.foreach(gf.contentType = _)
        gf.metaData = f.buildBSON
      }.map(_.asInstanceOf[ObjectId]))
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occured saving $f", e)
        None
    }.get
  }

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[File]
   */
  def get(fid: FileId): Option[File] = gfs.findOne(FileId.asObjId(fid)).map(fromGridFS)

  /**
   * "Moves" a file (including all versions) from one folder to another.
   *
   * @param cid CustomerId
   * @param filename String
   * @param orig Folder
   * @param mod Folder
   * @return An Option with the updated File
   */
  def move(cid: CustomerId, filename: String, orig: Path, mod: Path): Option[File] = {
    val q = MongoDBObject(
      "filename" -> filename,
      CidKey.full -> cid.value,
      PathKey.full -> orig.materialize
    )
    val upd = $set(PathKey.full -> mod.materialize)

    val res = collection.update(q, upd, multi = true)
    if (res.getN > 0) findLatest(cid, filename, Some(mod))
    else None // TODO: Handle this situation properly...
  }

  /**
   * Will return a collection of File (if found) with the provided filename and folder properties.
   *
   * @param cid CustomerId
   * @param filename String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def find(cid: CustomerId, filename: String, maybePath: Option[Path]): Seq[File] = {
    val fn = MongoDBObject("filename" -> filename, CidKey.full -> cid.value)
    val q = maybePath.fold(fn)(p => fn ++ MongoDBObject(PathKey.full -> p.materialize))
    val sort = MongoDBObject("uploadDate" -> -1)
    val query = MongoDBObject("$query" -> q, "$orderby" -> sort)

    gfs.find(query).map(f => fromGridFS(new GridFSDBFile(f)))
  }

  /**
   * Search for the latest version of a file matching the provided parameters.
   *
   * @param cid CustomerId
   * @param filename String
   * @param maybePath Option[Folder]
   * @return An Option containing the latest version of the File
   */
  def findLatest(cid: CustomerId, filename: String, maybePath: Option[Path]): Option[File] = {
    find(cid, filename, maybePath).headOption
  }

  /**
   * List all the files in the given Folder path
   *
   * @param path String
   * @return Option[File]
   */
  def listFiles(cid: CustomerId, path: String): Seq[File] = gfs.files(
    MongoDBObject(CidKey.full -> cid.value, PathKey.full -> path, IsFolderKey.full -> false)
  ).map(d => fromBSON(d)).toSeq

  /**
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return an Option with the UserId of the user holding the lock
   */
  def locked(fid: FileId): Option[UserId] = {
    get(fid).flatMap(fw => fw.metadata.lock.map(l => l.by))
  }

  /**
   * Places a lock on a file to prevent any modifications or new versions of the file
   *
   * @param uid UserId The id of the user that places the lock
   * @param fid FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will contain the applied lock.
   */
  def lock(uid: UserId, fid: FileId): LockOpStatus[_ <: Option[Lock]] = {
    // Only permit locking if not already locked
    locked(fid).map[LockOpStatus[Option[Lock]]](u => Locked(u)).getOrElse {
      val lock = Lock(uid, DateTime.now())
      val qry = MongoDBObject("_id" -> FileId.asObjId(fid))
      val upd = $set(LockKey.full -> Lock.toBSON(lock))

      Try {
        if (collection.update(qry, upd).getN > 0) Success(Option(lock))
        else Error("Locking query did not match any documents")
      }.recover {
        case e: Throwable => Error(s"An error occured trying to unlock $fid: ${e.getMessage}")
      }.get
    }
  }

  /**
   * Unlocks the provided file if and only if the provided user is the one holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlock(uid: UserId, fid: FileId): LockOpStatus[_ <: String] = {
    val qry = MongoDBObject("_id" -> FileId.asObjId(fid))
    val upd = $unset(LockKey.full)

    locked(fid).fold[LockOpStatus[_ <: String]](NotLocked())(usrId =>
      if (uid == usrId) {
        Try {
          val res = collection.update(qry, upd)
          if (res.getN > 0) Success(s"Successfully unlocked $fid")
          else Error("Unlocking query did not match any documents")
        }.recover {
          case e: Throwable => Error(s"An error occured trying to unlock $fid: ${e.getMessage}")
        }.get
      } else NotAllowed()
    )
  }
}