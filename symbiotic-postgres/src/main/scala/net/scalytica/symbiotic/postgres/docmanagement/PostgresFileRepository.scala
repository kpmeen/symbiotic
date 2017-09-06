package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.FileRepository
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.fs.FileSystemIO
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PostgresFileRepository(
    val config: Config,
    val fs: FileSystemIO
) extends FileRepository
    with SymbioticDb
    with SymbioticDbTables
    with SharedQueries {

  private val logger = LoggerFactory.getLogger(this.getClass)

  import profile.api._

  logger.debug(s"Initialized repository $getClass")

  private[this] def findLatestQuery(
      fid: FileId,
      owner: PartyId
  )(implicit ctx: SymbioticContext): DBIO[Option[FileRow]] = {
    val ap = ctx.accessibleParties.map(_.value)

    findLatestBaseQuery(_.filter { f =>
      f.ownerId === owner.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === false &&
      f.fileId === fid
    }).result.headOption
  }

  private[this] def findLatestQuery(
      fname: Option[String],
      mp: Option[Path],
      owner: PartyId
  )(implicit ctx: SymbioticContext): Query[FileTable, FileRow, Seq] = {
    val ap = ctx.accessibleParties.map(_.value)
    findLatestBaseQuery { query =>
      val q1 = filesTable.filter { f =>
        f.ownerId === owner.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === false
      }
      val q2 = fname.map(n => q1.filter(_.fileName === n)).getOrElse(q1)
      for {
        f1 <- mp.map(p => q2.filter(_.path === p)).getOrElse(q2)
        f2 <- query.groupBy(_.fileId).map(t => t._1 -> t._2.map(_.version).max)
        if f1.fileId === f2._1 && f1.version === f2._2
      } yield f1
    }
  }

  private[this] def findLatestQuery(
      fname: String,
      mp: Option[Path],
      owner: PartyId
  )(implicit ctx: SymbioticContext): Query[FileTable, FileRow, Seq] =
    findLatestQuery(Some(fname), mp, owner)

  private[this] def findQuery(
      fname: String,
      mp: Option[Path],
      owner: PartyId
  )(implicit ctx: SymbioticContext) = {
    val ap = ctx.accessibleParties.map(_.value)

    mp.map(p => filesTable.filter(_.path === p))
      .getOrElse(filesTable)
      .filter { f =>
        f.fileName === fname &&
        f.ownerId === owner.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === false
      }
      .sortBy(_.version.desc)
  }

  override def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    val row    = fileToRow(f)
    val action = (filesTable returning filesTable.map(_.id)) += row

    f.metadata.path.map { p =>
      editable(p).flatMap { isEditable =>
        if (isEditable) {
          db.run(action)
            .flatMap { res =>
              // Write the file to the persistent file store on disk
              val theFile = f.copy(id = Some(res))
              fs.write(theFile).map {
                case Right(()) =>
                  logger
                    .debug(s"File ${f.metadata.fid} with UUID $res was saved.")
                  Option(row._2)

                case Left(err) =>
                  logger.warn(err)
                  None
              }
            }
            .recover {
              case NonFatal(ex) =>
                logger.error(
                  s"An error occurred trying to persist file ${f.filename}"
                )
                throw ex
            }
        } else {
          logger.warn(
            s"Cannot upload File ${f.filename} to $p because its not editable"
          )
          Future.successful(None)
        }
      }
    }.getOrElse {
      logger.warn(s"Cannot save File ${f.filename} without a destination path")
      Future.successful(None)
    }
  }

  override def findLatestBy(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val query = findLatestQuery(fid, ctx.owner.id)
    db.run(query).map { res =>
      res.map { row =>
        val f = rowToFile(row)
        // Get a handle on the file from the persisted file store on disk
        f.copy(stream = fs.read(f))
      }
    }
  }

  override def move(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val ap = ctx.accessibleParties.map(_.value)

    val updQuery = filesTable.filter { f =>
      f.fileName === filename &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.path === orig &&
      f.isFolder === false
    }.map(_.path).update(mod)

    db.run(updQuery).flatMap { res =>
      if (res > 0) findLatest(filename, Some(mod))
      else Future.successful(None)
    }
  }

  override def find(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]] = {
    val query = findQuery(filename, maybePath, ctx.owner.id).result
    db.run(query).map(_.map(rowToFile))
  }

  override def findLatest(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val query =
      findLatestQuery(filename, maybePath, ctx.owner.id).result.headOption
    db.run(query).map(_.map(rowToFile))
  }

  override def listFiles(path: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]] = {
    val query = findLatestQuery(None, Some(path), ctx.owner.id)

    db.run(query.result).map(_.map(rowToFile))
  }

  override def lock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] = {
    val ap = ctx.accessibleParties.map(_.value)

    lockManagedFile(fid) { (dbId, lock) =>
      val upd = filesTable.filter { f =>
        f.id === dbId &&
        f.ownerId === ctx.owner.id.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === false
      }.map(r => (r.lockedBy, r.lockedDate))
        .update((Some(lock.by), Some(lock.date)))

      db.run(upd).map { res =>
        if (res > 0) LockApplied(Option(lock))
        else LockError("Locking query did not match any documents")
      }
    }
  }

  override def unlock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] = {
    val ap = ctx.accessibleParties.map(_.value)

    unlockManagedFile(fid) { dbId =>
      val upd = filesTable.filter { f =>
        f.id === dbId &&
        f.ownerId === ctx.owner.id.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === false
      }.map(r => (r.lockedBy, r.lockedDate)).update((None, None))

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

  override def editable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = {
    val query = editableQuery(from)

    db.run(query.result).map { rows =>
      if (rows.isEmpty) false
      else rows.map(rowToManagedFile).forall(_.metadata.lock.isEmpty)
    }
  }
}
