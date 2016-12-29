/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.docmanagement

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import com.mongodb.gridfs.{GridFSDBFile => MongoGridFSDBFile}
import models.docmanagement.Lock.LockOpStatusTypes._
import models.docmanagement.MetadataKeys._
import models.docmanagement.{File, FileId, Lock, Path}
import models.party.PartyBaseTypes.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.FileRepository
import repository.mongodb.bson.BSONConverters.Implicits._

import scala.util.Try

@Singleton
class MongoDBFileRepository @Inject() (
    val configuration: Configuration
) extends FileRepository with MongoFSRepository {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def save(f: File)(implicit uid: UserId): Option[FileId] = {
    val id = UUID.randomUUID()
    val fid = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))
    Try {
      f.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = file.filename
        file.contentType.foreach(gf.contentType = _)
        gf.metaData = managedfmd_toBSON(file.metadata)
        gf += ("_id" -> id.toString) // TODO: Verify this with the tests...
      }).map(_ => fid)
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $f", e)
        None
    }.toOption.flatten
  }

  override def get(id: UUID)(implicit uid: UserId): Option[File] =
    gfs.findOne(MongoDBObject("_id" -> id.toString))

  override def getLatest(fid: FileId)(implicit uid: UserId): Option[File] =
    collection.find(MongoDBObject(FidKey.full -> fid.value))
      .sort(MongoDBObject(VersionKey.full -> -1))
      .map(managedfile_fromBSON)
      .toSeq
      .headOption.flatMap(f => get(f.id.get))

  override def move(
    filename: String,
    orig: Path,
    mod: Path
  )(implicit uid: UserId): Option[File] = {
    val q = MongoDBObject(
      "filename" -> filename,
      OwnerKey.full -> uid.value,
      PathKey.full -> orig.materialize
    )
    val upd = $set(PathKey.full -> mod.materialize)

    val res = collection.update(q, upd, multi = true)
    if (res.getN > 0) findLatest(filename, Some(mod))
    else None // TODO: Handle this situation properly...
  }

  override def find(
    filename: String,
    maybePath: Option[Path]
  )(implicit uid: UserId): Seq[File] = {
    val fn = MongoDBObject("filename" -> filename, OwnerKey.full -> uid.value)
    val q = maybePath.fold(fn)(p => fn ++ MongoDBObject(PathKey.full -> p.materialize))
    val sort = MongoDBObject("uploadDate" -> -1)

    gfs.files(q).sort(sort).collect[File] {
      case f: DBObject =>
        file_fromGridFS(new GridFSDBFile(f.asInstanceOf[MongoGridFSDBFile]))
    }.toSeq
  }

  override def findLatest(
    filename: String,
    maybePath: Option[Path]
  )(implicit uid: UserId): Option[File] = find(filename, maybePath).headOption

  override def listFiles(path: String)(implicit uid: UserId): Seq[File] =
    gfs.files(
      MongoDBObject(
        OwnerKey.full -> uid.value,
        PathKey.full -> path,
        IsFolderKey.full -> false
      )
    ).map(d => file_fromBSON(d)).toSeq

  override def locked(fid: FileId)(implicit uid: UserId): Option[UserId] =
    getLatest(fid).flatMap(fw => fw.metadata.lock.map(l => l.by))

  private[this] def lockedAnd[A](
    uid: UserId,
    fid: FileId
  )(f: (Option[UserId], UUID) => A): Option[A] =
    getLatest(fid)(uid).map(file => f(file.metadata.lock.map(_.by), file.id.get))

  override def lock(
    fid: FileId
  )(implicit uid: UserId): LockOpStatus[_ <: Option[Lock]] = {
    // Only permit locking if not already locked
    lockedAnd(uid, fid) {
      case (maybeUid, oid) =>
        maybeUid.map[LockOpStatus[Option[Lock]]](Locked.apply).getOrElse {
          val lock = Lock(uid, DateTime.now())
          val qry = MongoDBObject(FidKey.full -> fid.value)
          val upd = $set(LockKey.full -> lock_toBSON(lock))

          Try {
            if (collection.update(qry, upd).getN > 0) LockApplied(Option(lock))
            else LockError("Locking query did not match any documents")
          }.recover {
            case e: Throwable =>
              LockError(s"An error occured trying to unlock $fid: ${e.getMessage}")

          }.get
        }
    }.getOrElse(LockError(s"File $fid was not found"))
  }

  override def unlock(fid: FileId)(implicit uid: UserId): LockOpStatus[_ <: String] = {
    lockedAnd(uid, fid) {
      case (maybeUid, id) =>
        maybeUid.fold[LockOpStatus[_ <: String]](NotLocked()) { usrId =>
          if (uid == usrId) {
            Try {
              val res = collection.update(
                MongoDBObject("_id" -> id.toString),
                $unset(LockKey.full)
              )
              if (res.getN > 0) LockApplied(s"Successfully unlocked $fid")
              else LockError("Unlocking query did not match any documents")
            }.recover {
              case e: Throwable =>
                LockError(s"An error occured trying to unlock $fid: ${e.getMessage}")

            }.get
          } else NotAllowed()
        }
    }.getOrElse(LockError(s"File $fid was not found"))
  }

}
