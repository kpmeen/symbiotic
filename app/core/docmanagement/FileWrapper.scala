/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.docmanagement

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.WithDateTimeConverters
import core.docmanagement.MetadataKeys._
import core.mongodb.{WithGridFS, WithMongoIndex}
import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

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
  filename: Option[String],
  contentType: Option[String] = None,
  uploadDate: Option[DateTime] = None,
  size: Long = 0, // Same as the length field in GridFS
  stream: Option[FileStream] = None,
  // The following fields will be added to the GridFS Metadata in fs.files...
  cid: CustomerId,
  pid: Option[ProjectId],
  uploadedBy: Option[UserId],
  version: Version = 1,
  folder: Option[Folder] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None) {

  /**
   * Build up the necessary metadata for persisting in GridFS
   */
  def getMetaData: Metadata = {
    val md1 = Map(
      CidKey.key -> cid.id,
      VersionKey.key -> version,
      IsFolderKey.key -> false
    )
    val md2 = uploadedBy.fold(md1)(u => md1 ++ Map(UploadedByKey.key -> u.id))
    val md3 = description.fold(md2)(d => md2 ++ Map(DescriptionKey.key -> d))
    lock.fold(md3)(l => md3 ++ Map(
      LockedByKey.partial -> l.by.id,
      LockDateKey.partial -> l.date.toDate
    ))
    val md4 = folder.fold(md3)(f => md3 ++ Map(PathKey.key -> f.materialize))
    val md5 = pid.fold(md4)(p => md4 ++ Map(PidKey.key -> p.id))
    md5
  }
}

object FileWrapper extends WithDateTimeConverters with WithGridFS with WithMongoIndex {

  implicit val fileReads: Reads[FileWrapper] = (
    (__ \ "id").readNullable[FileId] and
      (__ \ "filename").readNullable[String] and
      (__ \ "contentType").readNullable[String] and
      (__ \ "uploadDate").readNullable[DateTime] and
      (__ \ "size").read[Long] and
      (__ \ "stream").readNullable[FileStream](null) and
      (__ \ "cid").read[CustomerId] and
      (__ \ "pid").readNullable[ProjectId] and
      (__ \ "uploadedBy").readNullable[UserId] and
      (__ \ "version").read[Version] and
      (__ \ "folder").readNullable[Folder] and
      (__ \ "description").readNullable[String] and
      (__ \ "lock").readNullable[Lock]
    )(FileWrapper.apply _)

  implicit val fileWrites: Writes[FileWrapper] = (
    (__ \ "id").writeNullable[FileId] and
      (__ \ "filename").writeNullable[String] and
      (__ \ "contentType").writeNullable[String] and
      (__ \ "uploadDate").writeNullable[DateTime] and
      (__ \ "size").write[Long] and
      (__ \ "stream").writeNullable[FileStream](Writes.apply(s => JsNull)) and
      (__ \ "cid").write[CustomerId] and
      (__ \ "pid").writeNullable[ProjectId] and
      (__ \ "uploadedBy").writeNullable[UserId] and
      (__ \ "version").write[Version] and
      (__ \ "folder").writeNullable[Folder] and
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
      filename = gf.filename,
      contentType = gf.contentType,
      uploadDate = Option(gf.uploadDate),
      size = gf.length,
      stream = Option(gf.inputStream),
      cid = md.as[ObjectId](CidKey.key),
      pid = md.getAs[ObjectId](PidKey.key),
      uploadedBy = md.getAs[ObjectId](UploadedByKey.key),
      version = md.getAs[Int](VersionKey.key).getOrElse(1),
      folder = md.getAs[String](PathKey.key).map(p => Folder(p)),
      description = md.getAs[String](DescriptionKey.key),
      lock = md.getAs[MongoDBObject](LockKey.key).map(Lock.fromBSON)
    )
  }

  /**
   * Converter to map between a DBObject (from read operations) to a FileWrapper.
   * This will typically be used when listing files in a GridFS bucket.
   *
   * @param dbo DBObject
   * @return FileWrapper
   */
  def fromDBObject(dbo: DBObject): FileWrapper = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    FileWrapper(
      id = mdbo._id,
      filename = mdbo.getAs[String]("filename"),
      contentType = mdbo.getAs[String]("contentType"),
      uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
      size = mdbo.getAs[Long]("length").getOrElse(0),
      stream = None,
      // metadata
      cid = md.as[ObjectId](CidKey.key),
      pid = md.getAs[ObjectId](PidKey.key),
      uploadedBy = md.getAs[ObjectId](UploadedByKey.key),
      version = md.getAs[Int](VersionKey.key).getOrElse(1),
      folder = md.getAs[String](PathKey.key).map(p => Folder(p)),
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
    f.stream.flatMap(s => gfs(s) { gf =>
      f.filename.foreach(gf.filename = _)
      f.contentType.foreach(gf.contentType = _)
      gf.metaData = f.getMetaData
    }.map(_.asInstanceOf[ObjectId]))
  }

  /**
   * Will return a FileWrapper (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[FileWrapper]
   */
  def get(fid: FileId): Option[FileWrapper] = gfs.findOne(fid.id).map(fromGridFSFile)

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
    val query = maybePath.fold(fn)(p => fn ++ MongoDBObject(PathKey.full -> p.materialize))

    gfs.find(query).map(f => fromGridFSFile(new GridFSDBFile(f)))
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
   * Check if a file is locked or not. If called with uid = None (default) only the presence of a lock will be
   * inspected. Otherwise the uid will be used to compare the uid with the Lock.by value, and only if they are equal the
   * method will return true.
   *
   * @param file FileId
   * @param uid Option[UserId]
   * @return true if locked (and locked by)
   */
  def locked(file: FileId, uid: Option[UserId] = None): Boolean = {
    collection.findOne(MongoDBObject("_id" -> file.id)).flatMap(dbo =>
      dbo.getAs[MongoDBObject](MetadataKey).map(md =>
        md.getAs[MongoDBObject](LockKey.key).exists(l =>
          uid.fold(true)(u => l.getAs[ObjectId](LockedByKey.key).exists(bid => UserId(bid) == u))
        )
      )
    ).getOrElse(false)
  }

  /**
   * Places a lock on a file to prevent any modifications or new versions of the file
   *
   * @param uid UserId The id of the user that places the lock
   * @param file FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will contain the applied lock.
   */
  def lock(uid: UserId, file: FileId): Option[Lock] = {
    // Only permit locking if not already locked
    if (!locked(file)) {
      val lock = Lock(uid, DateTime.now())
      val qry = MongoDBObject("_id" -> file.id)
      val upd = $set(LockKey.full -> Lock.toBSON(lock))

      if (collection.update(qry, upd).getN > 0) Option(lock)
      else None // Lock was not possible for some reason
    } else {
      // File is already locked
      None
    }
  }

  /**
   * Unlocks the provided file if and only if the provided user is the one holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlock(uid: UserId, fid: FileId) = {
    val qry = MongoDBObject("_id" -> fid.id)
    val upd = $unset(LockKey.full)

    // TODO: Should differentiate between failed and already unlocked
    if (locked(fid, Some(uid))) {
      if (collection.update(qry, upd).getN > 0) true
      else false // Unlocking failed for some reason
    } else false // File isn't currently locked and requires no unlocking

  }

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param file FileWrapper
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok)
   */
  def serve(file: FileWrapper)(implicit ec: ExecutionContext): Result =
    file.stream.map(s => Results.Ok.chunked(Enumerator.fromStream(s))).getOrElse(Results.NotFound)

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