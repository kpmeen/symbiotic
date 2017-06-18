package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
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
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class MongoDBFileRepository(
    val configuration: Config
)(implicit as: ActorSystem, mat: Materializer)
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
      f.inputStream
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
      case NonFatal(e) =>
        logger.error(s"An error occurred trying to save $f", e)
        None
    }.toOption.flatten
  }

  private[this] def find(id: UUID)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = Future {
    gfs.findOne(MongoDBObject("_id" -> id.toString))
  }

  override def findLatestByFileId(fid: FileId)(
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
      .map(f => find(f.id.get))
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
    val sort = MongoDBObject(VersionKey.full -> -1) //("uploadDate" -> -1)

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

  override def listFiles(path: Path)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[File]] = Future {
    gfs
      .files(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          PathKey.full     -> path.materialize,
          IsFolderKey.full -> false
        )
      )
      .map(d => file_fromBSON(d))
      .toSeq
  }

  override def lock(fid: FileId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] = {
    lockFile(fid) {
      case (dbId, lock) =>
        Future {
          val qry = MongoDBObject(FidKey.full -> fid.value)
          val upd = $set(LockKey.full         -> lock_toBSON(lock))
          if (collection.update(qry, upd).getN > 0) {
            LockApplied(Option(lock))
          } else {
            val msg = "Locking query did not match any documents"
            logger.warn(msg)
            LockError(msg)
          }
        }
    }
  }

  override def unlock(fid: FileId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] = unlockFile(fid) { dbId =>
    Future {
      val res = collection.update(
        MongoDBObject("_id" -> dbId.toString),
        $unset(LockKey.full)
      )
      if (res.getN > 0) LockRemoved(s"Successfully unlocked $fid")
      else LockError("Unlocking query did not match any documents")
    }
  }

}
