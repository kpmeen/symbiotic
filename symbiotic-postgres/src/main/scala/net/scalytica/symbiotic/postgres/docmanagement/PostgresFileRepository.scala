package net.scalytica.symbiotic.postgres.docmanagement

import java.util.UUID

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FileRepository
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PostgresFileRepository(val config: Config)
    extends FileRepository
    with SymbioticDb
    with SymbioticDbTables {

  private val logger =
    LoggerFactory.getLogger(classOf[PostgresFileRepository])

  import profile.api._

  logger.debug(s"Initialized repository $getClass")

  override def save(f: File)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    val row    = fileToRow(f)
    val action = (filesTable returning filesTable.map(_.id)) += fileToRow(row)

    db.run(action).map { _ =>
      // TODO: Store f.stream to filesystem
      Option(row._2)
    }
  }

  private[this] def getLatestQuery(fid: FileId, owner: UserId) = {
    filesTable.filter { f =>
      f.fileId === fid && f.owner === owner && f.isFolder === false
    }.sortBy(_.version.desc).result.headOption
  }

  override def getLatest(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val query = getLatestQuery(fid, uid)
    db.run(query).map { res =>
      res.map { row =>
        // TODO: Get handle to actual file inputstream and add to the file!!!
        rowToFile(row)
      }
    }
  }

  override def move(filename: String, orig: Path, mod: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val updQuery = filesTable.filter { f =>
      f.fileName === filename && f.owner === uid && f.path === orig
    }.map(_.path).update(mod)

    db.run(updQuery).flatMap { res =>
      if (res > 0) findLatest(filename, Some(mod))
      else Future.successful(None)
    }
  }

  private def findQuery(fname: String, mp: Option[Path], owner: UserId) = {
    mp.map(p => filesTable.filter(_.path === p))
      .getOrElse(filesTable)
      .filter { f =>
        f.fileName === fname && f.owner === owner && f.isFolder === false
      }
      .sortBy(_.version.desc)
  }

  override def find(filename: String, maybePath: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[File]] = {
    val query = findQuery(filename, maybePath, uid).result
    db.run(query).map(_.map(rowToFile))
  }

  override def findLatest(filename: String, maybePath: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val query = findQuery(filename, maybePath, uid).result.headOption
    db.run(query).map(_.map(rowToFile))
  }

  override def listFiles(path: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[File]] = {
    val query = filesTable.filter { f =>
      f.path === path && f.owner === uid && f.isFolder === false
    }.result

    db.run(query).map(_.map(rowToFile))
  }

  override def lock(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] = lockFile(fid) {
    case (dbId, lock) =>
      val upd = filesTable
        .filter(_.id === dbId)
        .map(r => (r.lockedBy, r.lockedDate))
        .update((Some(lock.by), Some(lock.date)))

      db.run(upd).map { res =>
        if (res > 0) LockApplied(Option(lock))
        else LockError("Locking query did not match any documents")
      }
  }

  override def unlock(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] = unlockFile(fid) { dbId =>
    val upd = filesTable
      .filter(_.id === dbId)
      .map(r => (r.lockedBy, r.lockedDate))
      .update((None, None))

    db.run(upd).map {
      case res: Int if res > 0 =>
        LockRemoved(s"Successfully unlocked $fid")

      case _ =>
        val msg = "Unlocking query did not match any documents"
        logger.warn(msg)
        LockError(msg)
    }
  }
}
