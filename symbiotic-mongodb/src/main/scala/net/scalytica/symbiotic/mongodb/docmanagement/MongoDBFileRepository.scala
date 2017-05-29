package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import com.mongodb.gridfs.{GridFSDBFile => MongoGridFSDBFile}
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FileRepository
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MongoDBFileRepository(val configuration: Config)
    extends FileRepository
    with MongoFSRepository {

  private val logger = LoggerFactory.getLogger(this.getClass)

  logger.debug(
    s"Using configuration ${configuration.getConfig("symbiotic.mongodb")}"
  )

  override def save(f: File)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]] = Future {
    val id   = UUID.randomUUID()
    val fid  = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))
    Try {
      f.stream
        .flatMap(
          s =>
            gfs(s) { gf =>
              gf.filename = file.filename
              file.contentType.foreach(gf.contentType = _)
              gf.metaData = managedfmd_toBSON(file.metadata)
              gf += ("_id" -> id.toString) // TODO: Verify this with the tests
          }
        )
        .map(_ => fid)
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $f", e)
        None
    }.toOption.flatten
  }

  override def get(id: UUID)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = Future {
    gfs.findOne(MongoDBObject("_id" -> id.toString))
  }

  override def getLatest(fid: FileId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    logger.debug(s"Attempting to locate $fid")
    collection
      .find(MongoDBObject(FidKey.full -> fid.value))
      .sort(MongoDBObject(VersionKey.full -> -1))
      .map { dbo =>
        logger.debug(dbo.toString)
        managedfile_fromBSON(dbo)
      }
      .toSeq
      .headOption
      .map(f => get(f.id.get))
      .getOrElse(Future.successful(None))
  }

  override def move(filename: String, orig: Path, mod: Path)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val q = MongoDBObject(
      "filename"    -> filename,
      OwnerKey.full -> uid.value,
      PathKey.full  -> orig.materialize
    )
    val upd = $set(PathKey.full -> mod.materialize)

    val res = collection.update(q, upd, multi = true)
    if (res.getN > 0) findLatest(filename, Some(mod))
    else Future.successful(None) // TODO: Handle this situation properly...
  }

  override def find(filename: String, maybePath: Option[Path])(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[File]] = Future {
    val fn = MongoDBObject("filename" -> filename, OwnerKey.full -> uid.value)
    val q = maybePath.fold(fn)(
      p => fn ++ MongoDBObject(PathKey.full -> p.materialize)
    )
    val sort = MongoDBObject("uploadDate" -> -1)

    gfs
      .files(q)
      .sort(sort)
      .collect[File] {
        case f: DBObject =>
          file_fromGridFS(new GridFSDBFile(f.asInstanceOf[MongoGridFSDBFile]))
      }
      .toSeq
  }

  override def findLatest(filename: String, maybePath: Option[Path])(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = find(filename, maybePath).map(_.headOption)

  override def listFiles(path: String)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[File]] = Future {
    gfs
      .files(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          PathKey.full     -> path,
          IsFolderKey.full -> false
        )
      )
      .map(d => file_fromBSON(d))
      .toSeq
  }

  override def locked(fid: FileId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[UserId]] =
    getLatest(fid).map(_.flatMap(fw => fw.metadata.lock.map(l => l.by)))

  private[this] def lockedAnd[A](uid: UserId, fid: FileId)(
      f: (Option[UserId], UUID) => A
  )(implicit tu: TransUserId, ec: ExecutionContext): Future[Option[A]] =
    getLatest(fid)(uid, tu, ec)
      .map(_.map(file => f(file.metadata.lock.map(_.by), file.id.get)))

  override def lock(fid: FileId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] = {
    // Only permit locking if not already locked
    lockedAnd(uid, fid) {
      case (maybeUid, oid) =>
        maybeUid.map[LockOpStatus[Option[Lock]]](Locked.apply).getOrElse {
          val lock = Lock(uid, DateTime.now())
          val qry  = MongoDBObject(FidKey.full -> fid.value)
          val upd  = $set(LockKey.full -> lock_toBSON(lock))

          Try {
            if (collection.update(qry, upd).getN > 0) {
              LockApplied(Option(lock))
            } else {
              val msg = "Locking query did not match any documents"
              logger.warn(msg)
              LockError(msg)
            }
          }.recover {
            case e: Throwable =>
              val msg =
                s"An error occured trying to unlock $fid: ${e.getMessage}"
              logger.error(msg, e)
              LockError(msg)
          }.get
        }
    }.map(_.getOrElse {
      val msg = s"Cannot apply lock because file $fid was not found"
      logger.debug(msg)
      LockError(msg)
    })
  }

  override def unlock(fid: FileId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] = {
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
                LockError(
                  s"An error occured trying to unlock $fid: ${e.getMessage}"
                )

            }.get
          } else NotAllowed()
        }
    }.map(_.getOrElse(LockError(s"File $fid was not found")))
  }

}
