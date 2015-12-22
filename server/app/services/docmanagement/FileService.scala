/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.docmanagement

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import models.docmanagement.File._
import models.docmanagement.Lock.LockOpStatusTypes._
import models.docmanagement.MetadataKeys._
import models.docmanagement.{File, FileId, Lock, Path}
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.util.Try

object FileService extends ManagedFileService {

  val logger = LoggerFactory.getLogger(FileService.getClass)

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param f File
   * @return Option[FileId]
   */
  def save(f: File): Option[FileId] = {
    val fid = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))
    Try {
      f.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = file.filename
        file.contentType.foreach(gf.contentType = _)
        gf.metaData = file.metadata
      }.flatMap(_ => Some(fid)))
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $f", e)
        None
    }.toOption.flatten
  }

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param oid ObjectId
   * @return Option[File]
   */
  def get(oid: ObjectId): Option[File] = gfs.findOne(oid)

  def getLatest(fid: FileId): Option[File] =
    collection.find(MongoDBObject(FidKey.full -> fid.value))
      .sort(MongoDBObject(VersionKey.full -> -1))
      .map(File.fromBSON)
      .toSeq
      .headOption.flatMap(f => get(f.id.get))

  /**
   * "Moves" a file (including all versions) from one folder to another.
   *
   * @param oid OrgId
   * @param filename String
   * @param orig Folder
   * @param mod Folder
   * @return An Option with the updated File
   */
  def move(oid: OrganisationId, filename: String, orig: Path, mod: Path): Option[File] = {
    val q = MongoDBObject(
      "filename" -> filename,
      OidKey.full -> oid.value,
      PathKey.full -> orig.materialize
    )
    val upd = $set(PathKey.full -> mod.materialize)

    val res = collection.update(q, upd, multi = true)
    if (res.getN > 0) findLatest(oid, filename, Some(mod))
    else None // TODO: Handle this situation properly...
  }

  /**
   * Will return a collection of File (if found) with the provided filename and folder properties.
   *
   * @param oid OrgId
   * @param filename String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def find(oid: OrganisationId, filename: String, maybePath: Option[Path]): Seq[File] = {
    val fn = MongoDBObject("filename" -> filename, OidKey.full -> oid.value)
    val q = maybePath.fold(fn)(p => fn ++ MongoDBObject(PathKey.full -> p.materialize))
    val sort = MongoDBObject("uploadDate" -> -1)
    val qry = MongoDBObject("$query" -> q, "$orderby" -> sort)

    gfs.find(qry).map(f => fromGridFS(new GridFSDBFile(f)))
  }

  /**
   * Search for the latest version of a file matching the provided parameters.
   *
   * @param oid OrgId
   * @param filename String
   * @param maybePath Option[Folder]
   * @return An Option containing the latest version of the File
   */
  def findLatest(oid: OrganisationId, filename: String, maybePath: Option[Path]): Option[File] = {
    find(oid, filename, maybePath).headOption
  }

  /**
   * List all the files in the given Folder path
   *
   * @param path String
   * @return Option[File]
   */
  def listFiles(oid: OrganisationId, path: String): Seq[File] = gfs.files(
    MongoDBObject(OidKey.full -> oid.value, PathKey.full -> path, IsFolderKey.full -> false)
  ).map(d => fromBSON(d)).toSeq

  /**
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return an Option with the UserId of the user holding the lock
   */
  def locked(fid: FileId): Option[UserId] = getLatest(fid).flatMap(fw => fw.metadata.lock.map(l => l.by))

  def lockedAnd[A](fid: FileId)(f: (Option[UserId], ObjectId) => A): Option[A] =
    getLatest(fid).map(file => f(file.metadata.lock.map(_.by), file.id.get))

  /**
   * Places a lock on a file to prevent any modifications or new versions of the file
   *
   * @param uid UserId The id of the user that places the lock
   * @param fid FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will contain the applied lock.
   */
  // TODO: Refactor to use getLatest
  def lock(uid: UserId, fid: FileId): LockOpStatus[_ <: Option[Lock]] = {
    // Only permit locking if not already locked
    lockedAnd(fid) {
      case (maybeUid, oid) =>
        maybeUid.map[LockOpStatus[Option[Lock]]](Locked.apply).getOrElse {
          val lock = Lock(uid, DateTime.now())
          val qry = MongoDBObject(FidKey.full -> fid.value)
          val upd = $set(LockKey.full -> Lock.toBSON(lock))

          Try {
            if (collection.update(qry, upd).getN > 0) Success(Option(lock))
            else Error("Locking query did not match any documents")
          }.recover {
            case e: Throwable => Error(s"An error occured trying to unlock $fid: ${e.getMessage}")
          }.get
        }
    }.getOrElse(Error(s"File $fid was not found"))
  }

  /**
   * Unlocks the provided file if and only if the provided user is the one holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlock(uid: UserId, fid: FileId): LockOpStatus[_ <: String] = {
    lockedAnd(fid) {
      case (maybeUid, oid) =>
        maybeUid.fold[LockOpStatus[_ <: String]](NotLocked()) { usrId =>
          if (uid == usrId) {
            Try {
              val res = collection.update(MongoDBObject("_id" -> oid), $unset(LockKey.full))
              if (res.getN > 0) Success(s"Successfully unlocked $fid")
              else Error("Unlocking query did not match any documents")
            }.recover {
              case e: Throwable => Error(s"An error occured trying to unlock $fid: ${e.getMessage}")
            }.get
          } else NotAllowed()
        }
    }.getOrElse(Error(s"File $fid was not found"))
  }

}
