/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.docmanagement

import com.google.inject.Singleton
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import models.docmanagement.Lock.LockOpStatusTypes._
import models.docmanagement.MetadataKeys._
import models.docmanagement.{File, FileId, Lock, Path}
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import repository.FileRepository
import repository.mongodb.bson.BSONConverters.Implicits._

import scala.util.Try

@Singleton
class MongoDBFileRepository extends FileRepository[ObjectId] with MongoFSRepository {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def save(f: File): Option[FileId] = {
    val fid = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))
    Try {
      f.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = file.filename
        file.contentType.foreach(gf.contentType = _)
        gf.metaData = managedfmd_toBSON(file.metadata)
      }.flatMap(_ => Some(fid)))
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $f", e)
        None
    }.toOption.flatten
  }

  override def get(oid: ObjectId): Option[File] = gfs.findOne(oid)

  override def getLatest(fid: FileId): Option[File] =
    collection.find(MongoDBObject(FidKey.full -> fid.value))
      .sort(MongoDBObject(VersionKey.full -> -1))
      .map(managedfile_fromBSON)
      .toSeq
      .headOption.flatMap(f => get(f.id.get))

  override def move(oid: OrganisationId, filename: String, orig: Path, mod: Path): Option[File] = {
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

  override def find(oid: OrganisationId, filename: String, maybePath: Option[Path]): Seq[File] = {
    val fn = MongoDBObject("filename" -> filename, OidKey.full -> oid.value)
    val q = maybePath.fold(fn)(p => fn ++ MongoDBObject(PathKey.full -> p.materialize))
    val sort = MongoDBObject("uploadDate" -> -1)
    val qry = MongoDBObject("$query" -> q, "$orderby" -> sort)

    gfs.find(qry).map(f => file_fromGridFS(new GridFSDBFile(f)))
  }

  override def findLatest(oid: OrganisationId, filename: String, maybePath: Option[Path]): Option[File] = {
    find(oid, filename, maybePath).headOption
  }

  override def listFiles(oid: OrganisationId, path: String): Seq[File] = gfs.files(
    MongoDBObject(OidKey.full -> oid.value, PathKey.full -> path, IsFolderKey.full -> false)
  ).map(d => file_fromBSON(d)).toSeq

  override def locked(fid: FileId): Option[UserId] = getLatest(fid).flatMap(fw => fw.metadata.lock.map(l => l.by))

  private[this] def lockedAnd[A](fid: FileId)(f: (Option[UserId], ObjectId) => A): Option[A] =
    getLatest(fid).map(file => f(file.metadata.lock.map(_.by), file.id.get))

  override def lock(uid: UserId, fid: FileId): LockOpStatus[_ <: Option[Lock]] = {
    // Only permit locking if not already locked
    lockedAnd(fid) {
      case (maybeUid, oid) =>
        maybeUid.map[LockOpStatus[Option[Lock]]](Locked.apply).getOrElse {
          val lock = Lock(uid, DateTime.now())
          val qry = MongoDBObject(FidKey.full -> fid.value)
          val upd = $set(LockKey.full -> lock_toBSON(lock))

          Try {
            if (collection.update(qry, upd).getN > 0) Success(Option(lock))
            else Error("Locking query did not match any documents")
          }.recover {
            case e: Throwable => Error(s"An error occured trying to unlock $fid: ${e.getMessage}")
          }.get
        }
    }.getOrElse(Error(s"File $fid was not found"))
  }

  override def unlock(uid: UserId, fid: FileId): LockOpStatus[_ <: String] = {
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
