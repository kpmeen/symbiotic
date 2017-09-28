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

  private val log = LoggerFactory.getLogger(this.getClass)

  import profile.api._

  log.debug(s"Initialized repository $getClass")

  private[this] def findLatestQuery(
      fid: FileId,
      owner: PartyId
  )(implicit ctx: SymbioticContext): Query[FileTable, FileRow, Seq] = {
    findLatestBaseQuery(_.filter { f =>
      f.ownerId === owner.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === false &&
      f.fileId === fid &&
      f.isDeleted === false
    })
  }

  private[this] def findLatestAction(
      fid: FileId,
      owner: PartyId
  )(implicit ctx: SymbioticContext): DBIO[Option[FileRow]] = {
    findLatestQuery(fid, owner).result.headOption
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
        f.isFolder === false &&
        f.isDeleted === false
      }
      val q2 = fname.map(n => q1.filter(_.fileName === n)).getOrElse(q1)
      for {
        f1 <- mp.map(p => q2.filter(_.path === p)).getOrElse(q2)
        f2 <- query.groupBy(_.fileId).map(t => t._1 -> t._2.map(_.version).max)
        if f1.fileId === f2._1 && f1.version === f2._2
      } yield f1
    }
  }

  private[this] def insertAction(row: FileRow) = {
    (filesTable returning filesTable.map(_.id)) += row
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
        f.isFolder === false &&
        f.isDeleted === false
      }
      .sortBy(_.version.desc)
  }

  private[this] def insert(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ) = {
    val row = fileToRow(f)
    db.run(insertAction(row).transactionally)
      .flatMap { uuid =>
        // Write the file to the persistent file store on disk
        val theFile = f.copy(id = Some(uuid))
        fs.write(theFile).map {
          case Right(()) =>
            log.debug(s"Saved file ${f.metadata.fid} with UUID $uuid.")
            Option(row._2)

          case Left(err) =>
            log.warn(err)
            None
        }
      }
      .recover {
        case NonFatal(ex) =>
          log.error(
            s"An error occurred trying to persist file ${f.filename}"
          )
          throw ex
      }
  }

  private[this] def update(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    f.metadata.fid.map { fid =>
      val extAttrs = f.metadata.extraAttributes.map(metadataMapToJson)
      val q1       = filesTable.filter(_.id === f.id)
      val updAction = q1.map { r =>
        (r.description, r.customMetadata)
      }.update((f.metadata.description, extAttrs))

      db.run(updAction.transactionally).flatMap {
        case numUpd: Int if numUpd > 0 =>
          log.debug(s"Successfully updated $fid")
          findLatestBy(fid)

        case _ =>
          log.warn(
            s"Update of ${f.metadata.fid} named ${f.filename} didn't change" +
              " any data"
          )
          Future.successful(None)
      }
    }.getOrElse {
      log.warn(
        s"Attempted update of ${f.filename} at ${f.metadata.path} without " +
          "providing its FileId and unique ID"
      )
      Future.successful(None)
    }
  }

  override def updateMetadata(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] =
    f.metadata.path.map { p =>
      editable(p).flatMap { isEditable =>
        if (isEditable) {
          update(f)
        } else {
          log.warn(
            s"Can't update metadata for File ${f.filename} because $p is " +
              "not editable"
          )
          Future.successful(None)
        }
      }
    }.getOrElse {
      log.warn(
        s"Can't update metadata for File ${f.filename} without a " +
          "destination path"
      )
      Future.successful(None)
    }

  override def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] =
    f.metadata.path.map { p =>
      editable(p).flatMap { isEditable =>
        if (isEditable) {
          insert(f)
        } else {
          log.warn(s"Can't save File ${f.filename} because $p is not editable")
          Future.successful(None)
        }
      }
    }.getOrElse {
      log.warn(s"Can't save File ${f.filename} without a destination path")
      Future.successful(None)
    }

  override def findLatestBy(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    val query = findLatestAction(fid, ctx.owner.id)
    db.run(query).map { res =>
      res.map { row =>
        val f = rowToFile(row)
        log.debug(s"Found file ${f.filename} with id ${f.metadata.fid}")
        // Get a handle on the file from the persisted file store on disk
        val stream = fs.read(f)
        log.debug(s"Stream for ${f.filename} is $stream")
        f.copy(stream = stream)
      }
    }
  }

  override def move(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    editable(orig).flatMap { isEditable =>
      findLatest(filename, Some(orig)).flatMap {
        case Some(f) =>
          if (f.metadata.lock.forall(_.by == ctx.currentUser)) {
            val updQuery = filesTable.filter { f =>
              f.fileName === filename &&
              f.ownerId === ctx.owner.id.value &&
              accessiblePartiesFilter(f, ctx.accessibleParties) &&
              f.path === orig &&
              f.isFolder === false
            }.map(_.path).update(mod)

            db.run(updQuery.transactionally).flatMap { res =>
              if (res > 0) findLatest(filename, Some(mod))
              else Future.successful(None)
            }
          } else {
            log.info(s"$filename is locked by another user and can't be moved")
            Future.successful(None)
          }

        case None =>
          Future.successful(None)
      }
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
    db.run(query).map { res =>
      res.map { row =>
        val f = rowToFile(row)
        log.debug(s"Found file ${f.filename} with id ${f.metadata.fid}")
        // Get a handle on the file from the persisted file store on disk
        val stream = fs.read(f)
        log.debug(s"Stream for ${f.filename} is $stream")
        f.copy(stream = stream)
      }
    }
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

      db.run(upd.transactionally).map { res =>
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

      db.run(upd.transactionally).map {
        case res: Int if res > 0 =>
          LockRemoved(s"Successfully unlocked $fid")

        case _ =>
          val msg = "Unlocking query did not match any documents"
          log.warn(msg)
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

  override def markAsDeleted(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Either[String, Int]] = {
    val action = filesTable.filter { f =>
      f.fileId === fid &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === false &&
      f.isDeleted === false
    }.map(_.isDeleted).update(true)

    db.run(action.transactionally)
      .map {
        case res: Int if res > 0 =>
          Right(res)

        case res: Int if res == 0 =>
          val msg = s"File $fid was not marked as deleted"
          log.debug(msg)
          Left(msg)
      }
      .recover {
        case NonFatal(ex) =>
          val msg = s"An error occurred marking $fid as deleted"
          log.error(msg, ex)
          Left(msg)
      }
  }

  override def eraseFile(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Either[String, Int]] = {
    val baseQry = filesTable.filter { f =>
      f.fileId === fid &&
      f.ownerId === ctx.owner.id.value &&
      f.isFolder === false &&
      accessiblePartiesFilter(f, ctx.accessibleParties)
    }

    val action = for {
      files <- baseQry.result.map { rows =>
                rows.map(rowToFile).map { f =>
                  val erased = fs.eraseFile(f)
                  if (erased)
                    log.debug(
                      s"Successfully erased file ${f.filename} version" +
                        s" ${f.metadata.version}"
                    )
                  else
                    log.warn(
                      s"Could not erase file ${f.filename} version" +
                        s" ${f.metadata.version}"
                    )
                  f.id
                }
              }
      delete <- baseQry.delete
    } yield {
      delete
    }

    db.run(action.transactionally).map {
      case res: Int if res > 0 =>
        Right(res)

      case res: Int if res == 0 =>
        val msg = s"File $fid was not fully erased"
        log.debug(msg)
        Left(msg)
    }
  }

}
