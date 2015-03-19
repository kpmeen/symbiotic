/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.docmanagement

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.WithDateTimeConverters
import core.docmanagement.Lock.LockOpStatusTypes._
import core.docmanagement.MetadataKeys._
import core.mongodb.{WithGridFS, WithMongoIndex}
import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Represents a file to be up/down -loaded by a User.
 *
 * This is <i>NOT</i> a file in the sense of a java.util.File. But rather a wrapper around an InputStream with
 * quite a bit of extra Metadata information. The Metadata is mapped to the GridFS "<bucket>.files" collection, and
 * the InputStream is read from the "<bucket>.chunks" collection.
 *
 */
case class FileWrapper(
  id: Option[FileId] = None,
  filename: String,
  contentType: Option[String] = None,
  uploadDate: Option[DateTime] = None,
  size: Long = 0, // Same as the length field in GridFS
  stream: Option[FileStream] = None,
  // The following fields will be added to the GridFS Metadata in fs.files...
  cid: CustomerId,
  pid: Option[ProjectId],
  uploadedBy: Option[UserId],
  version: Version = 1,
  isFolder: Option[Boolean] = None,
  folder: Option[Folder] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None) {

  /**
   * Feeds the InputStream bytes into an Enumerator
   */
  def enumerate(implicit ec: ExecutionContext): Option[Enumerator[Array[Byte]]] = stream.map(s => Enumerator.fromStream(s))

  /**
   * Build up the necessary metadata for persisting in GridFS
   */
  def buildBSONMetaData: Metadata = {
    val md1 = MongoDBObject(
      CidKey.key -> cid.id,
      VersionKey.key -> version,
      IsFolderKey.key -> false
    )
    val md2 = uploadedBy.fold(md1)(u => md1 ++ MongoDBObject(UploadedByKey.key -> u.id))
    val md3 = description.fold(md2)(d => md2 ++ MongoDBObject(DescriptionKey.key -> d))
    val md4 = lock.fold(md3)(l => md3 ++ MongoDBObject(LockKey.key -> MongoDBObject(
      LockByKey.key -> l.by.id,
      LockDateKey.key -> l.date.toDate
    )))
    val md5 = folder.fold(md4)(f => md4 ++ MongoDBObject(PathKey.key -> f.materialize))
    val md6 = pid.fold(md5)(p => md5 ++ MongoDBObject(PidKey.key -> p.id))
    md6
  }
}

object FileWrapper extends WithDateTimeConverters with WithGridFS with WithMongoIndex {

  val logger = LoggerFactory.getLogger(FileWrapper.getClass)

  implicit val fwReads: Reads[FileWrapper] = (
    (__ \ "id").readNullable[FileId] and
      (__ \ "filename").read[String] and
      (__ \ "contentType").readNullable[String] and
      (__ \ "uploadDate").readNullable[DateTime] and
      (__ \ "size").read[Long] and
      (__ \ "stream").readNullable[FileStream](null) and
      (__ \ "cid").read[CustomerId] and
      (__ \ "pid").readNullable[ProjectId] and
      (__ \ "uploadedBy").readNullable[UserId] and
      (__ \ "version").read[Version] and
      (__ \ "isFolder").readNullable[Boolean] and
      (__ \ "folder").readNullable[Folder](Folder.folderReads) and
      (__ \ "description").readNullable[String] and
      (__ \ "lock").readNullable[Lock]
    )(FileWrapper.apply _)

  implicit val fwWrites: Writes[FileWrapper] = (
    (__ \ "id").writeNullable[FileId] and
      (__ \ "filename").write[String] and
      (__ \ "contentType").writeNullable[String] and
      (__ \ "uploadDate").writeNullable[DateTime] and
      (__ \ "size").write[Long] and
      (__ \ "stream").writeNullable[FileStream](Writes.apply(s => JsNull)) and
      (__ \ "cid").write[CustomerId] and
      (__ \ "pid").writeNullable[ProjectId] and
      (__ \ "uploadedBy").writeNullable[UserId] and
      (__ \ "version").write[Version] and
      (__ \ "isFolder").writeNullable[Boolean] and
      (__ \ "folder").writeNullable[Folder](Folder.folderWrites) and
      (__ \ "description").writeNullable[String] and
      (__ \ "lock").writeNullable[Lock]
    )(unlift(FileWrapper.unapply))

  override def ensureIndex(): Unit = {
    // TODO: Only create indices if they don't already exist!!!
    val background = MongoDBObject("background" -> true)
    collection.createIndex(MongoDBObject("filename" -> 1), background)
    collection.createIndex(MongoDBObject(CidKey.full -> 1), background)
    collection.createIndex(MongoDBObject(UploadedByKey.full -> 1), background)
    collection.createIndex(MongoDBObject(PathKey.full -> 1), background)
    collection.createIndex(MongoDBObject(VersionKey.full -> 1), background)
    collection.createIndex(MongoDBObject(IsFolderKey.full -> 1), background)
  }

  /**
   * Converter to map between a GridFSDBFile (from read operations) to a FileWrapper
   *
   * @param gf GridFSDBFile
   * @return FileWrapper
   */
  def fromGridFSFile(gf: GridFSDBFile): FileWrapper = {
    val md = new MongoDBObject(gf.metaData)
    FileWrapper(
      id = gf._id,
      filename = gf.filename.getOrElse("no_name"),
      contentType = gf.contentType,
      uploadDate = Option(gf.uploadDate),
      size = gf.length,
      stream = Option(gf.inputStream),
      cid = md.as[ObjectId](CidKey.key),
      pid = md.getAs[ObjectId](PidKey.key),
      uploadedBy = md.getAs[ObjectId](UploadedByKey.key),
      version = md.getAs[Int](VersionKey.key).getOrElse(1),
      isFolder = md.getAs[Boolean](IsFolderKey.key),
      folder = md.getAs[String](PathKey.key).map(Folder.apply),
      description = md.getAs[String](DescriptionKey.key),
      lock = md.getAs[MongoDBObject](LockKey.key).map(Lock.fromBSON)
    )
  }

  /**
   * Converter to map between a DBObject (from read operations) to a FileWrapper.
   * This will typically be used when listing files in a GridFS <bucket>.files collection
   *
   * @param dbo DBObject
   * @return FileWrapper
   */
  def fromDBObject(dbo: DBObject): FileWrapper = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    FileWrapper(
      id = mdbo._id,
      filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
      contentType = mdbo.getAs[String]("contentType"),
      uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
      size = mdbo.getAs[Long]("length").getOrElse(0),
      stream = None,
      // metadata
      cid = md.as[ObjectId](CidKey.key),
      pid = md.getAs[ObjectId](PidKey.key),
      uploadedBy = md.getAs[ObjectId](UploadedByKey.key),
      version = md.getAs[Int](VersionKey.key).getOrElse(1),
      isFolder = md.getAs[Boolean](IsFolderKey.key),
      folder = md.getAs[String](PathKey.key).map(Folder.apply),
      description = md.getAs[String](DescriptionKey.key),
      lock = md.getAs[MongoDBObject](LockKey.key).map(Lock.fromBSON)
    )
  }

  /**
   * Saves the passed on FileWrapper in MongoDB GridFS
   *
   * @param f FileWrapper
   * @return Option[ObjectId]
   */
  def save(f: FileWrapper): Option[FileId] = {
    Try {
      f.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = f.filename
        f.contentType.foreach(gf.contentType = _)
        gf.metaData = f.buildBSONMetaData
      }.map(_.asInstanceOf[ObjectId]))
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occured saving $f", e)
        None
    }.get
  }

  /**
   * Will return a FileWrapper (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[FileWrapper]
   */
  def get(fid: FileId): Option[FileWrapper] = gfs.findOne(fid.id).map(fromGridFSFile)

  /**
   * TODO: Document me...
   *
   * @param cid CustomerId
   * @param filename String
   * @param orig Folder
   * @param mod Folder
   * @return
   */
  def move(cid: CustomerId, filename: String, orig: Folder, mod: Folder): Option[FileWrapper] = {
    val q = MongoDBObject(
      "filename" -> filename,
      CidKey.full -> cid.id,
      PathKey.full -> orig.materialize
    )
    val u = $set(PathKey.full -> mod.materialize)

    val res = collection.update(q, u, multi = true)
    if (res.getN > 0) {
      findLatest(cid, filename, Some(mod))
    } else {
      // TODO: Handle this situation properly...
      None
    }
  }

  /**
   * Will return a collection of FileWrapper (if found) with the provided filename and folder properties.
   *
   * @param cid CustomerId
   * @param filename String
   * @param maybePath Option[Path]
   * @return Seq[FileWrapper]
   */
  def find(cid: CustomerId, filename: String, maybePath: Option[Folder]): Seq[FileWrapper] = {
    val fn = MongoDBObject("filename" -> filename, CidKey.full -> cid.id)
    val q = maybePath.fold(fn)(p => fn ++ MongoDBObject(PathKey.full -> p.materialize))
    val sort = MongoDBObject("uploadDate" -> -1)
    val query = MongoDBObject("$query" -> q, "$orderby" -> sort)

    gfs.find(query).map(f => fromGridFSFile(new GridFSDBFile(f)))
  }

  /**
   * Search for the latest version of a file matching the provided parameters.
   *
   * @param cid CustomerId
   * @param filename String
   * @param maybePath Option[Folder]
   * @return An Option containing the latest version of the FileWrapper
   */
  def findLatest(cid: CustomerId, filename: String, maybePath: Option[Folder]): Option[FileWrapper] = {
    find(cid, filename, maybePath).headOption
  }

  /**
   * List all the files in the given Folder path
   *
   * @param path String
   * @return Option[FileWrapper]
   */
  def listFiles(cid: CustomerId, path: String): Seq[FileWrapper] = gfs.files(
    MongoDBObject(CidKey.full -> cid.id, PathKey.full -> path, IsFolderKey.full -> false)
  ).map(d => fromDBObject(d)).toSeq

  /**
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return an Option with the UserId of the user holding the lock
   */
  def locked(fid: FileId): Option[UserId] = {
    get(fid).flatMap(fw => fw.lock.map(l => l.by))
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
      val qry = MongoDBObject("_id" -> fid.id)
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
    val qry = MongoDBObject("_id" -> fid.id)
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

  // TODO: These should better live in a file controller or similar
  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param file FileWrapper
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok)
   */
  def serve(file: FileWrapper)(implicit ec: ExecutionContext): Result =
    file.enumerate.map(fenum => Results.Ok.chunked(fenum)).getOrElse(Results.NotFound)

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param maybeFile Option[FileWrapper]
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok or NotFound)
   */
  def serve(maybeFile: Option[FileWrapper])(implicit ec: ExecutionContext): Result =
    maybeFile.map(serve).getOrElse(Results.NotFound)

  /**
   * Serves a Future file by streaming the content back as chunks to the client.
   *
   * @param futureFile Future[FileWrapper]
   * @param ec ExecutionContext required due to using Futures
   * @return Future[Result] (Ok)
   */
  def serve(futureFile: Future[FileWrapper])(implicit ec: ExecutionContext): Future[Result] =
    futureFile.map(serve)
}