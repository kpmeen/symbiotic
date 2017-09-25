package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.FolderRepository
import net.scalytica.symbiotic.api.types.CommandStatusTypes._
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes.{
  LockApplied,
  LockError,
  LockOpStatus,
  LockRemoved
}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PostgresFolderRepository(
    val config: Config
) extends FolderRepository
    with SymbioticDb
    with SymbioticDbTables
    with SharedQueries {

  private val log = LoggerFactory.getLogger(this.getClass)

  import profile.api._

  log.debug(s"Initialized repository $getClass")

  private def insertAction(row: FileRow) = {
    (filesTable returning filesTable.map(_.id)) += row
  }

  private def updateAction(f: Folder) = {
    filesTable
      .filter(r => r.id === f.id && r.isFolder === true)
      .map { row =>
        (
          row.contentType,
          row.description,
          row.createdDate,
          row.customMetadata
        )
      }
      .update(
        (
          f.fileType,
          f.metadata.description,
          f.createdDate,
          f.metadata.extraAttributes.map(metadataMapToJson)
        )
      )
  }

  override def save(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    exists(f).flatMap {
      case fe: Boolean if !fe =>
        log.debug(s"Folder at ${f.flattenPath} will be created.")
        val row = folderToRow(f)
        db.run(insertAction(row).transactionally).map(_ => Option(row._2))

      case fe: Boolean if fe && Path.root != f.flattenPath =>
        log.debug(s"Folder at ${f.flattenPath} will be updated.")
        db.run(updateAction(f).transactionally).map(_ => f.metadata.fid)

      case fe: Boolean if fe =>
        log.debug(s"Folder at ${f.flattenPath} already exists.")
        Future.successful(None)
    }
  }

  /*
    TODO: The current implementation is rather naive and just calls `get(fid)`.
    This won't be enough once folders supports a version scheme.
   */
  override def findLatestBy(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = get(fid)

  override def get(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = {
    val query = filesTable.filter { f =>
      f.fileId === folderId &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.result.headOption

    db.run(query).map(_.map(rowToFolder))
  }

  override def get(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = {
    val query = filesTable.filter { f =>
      f.path === at &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.result.headOption

    db.run(query).map(_.map(rowToFolder))
  }

  override def exists(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = {
    val query = filesTable.filter { f =>
      f.path === at &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.exists.result

    db.run(query)
  }

  override def filterMissing(p: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[List[Path]] = {
    val allPaths =
      p.value.split("/").filterNot(_.isEmpty).foldLeft(List.empty[Path]) {
        case (paths, curr) =>
          paths :+ paths.lastOption
            .map(l => Path(s"${l.value}/$curr"))
            .getOrElse(Path.root)
      }

    val query = filesTable.filter { f =>
      (f.path inSet allPaths) &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }

    db.run(query.result).map { res =>
      val folders = res.map(rowToFolder)
      allPaths.filterNot(p => folders.map(_.flattenPath).contains(p))
    }
  }

  override def move(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[CommandStatus[Int]] = {
    val action = filesTable.filter { f =>
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.path === orig &&
      f.isFolder === true &&
      f.isDeleted === false
    }.map(f => (f.fileName, f.path)).update((mod.nameOfLast, mod))

    db.run(action.transactionally)
      .map(r => if (r > 0) CommandOk(r) else CommandKo(0))
      .recover {
        case NonFatal(ex) => CommandError(0, Option(ex.getMessage))
      }
  }

  override def lock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] =
    lockManagedFile(fid) { (dbId, lock) =>
      val upd = filesTable.filter { f =>
        f.id === dbId &&
        f.ownerId === ctx.owner.id.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === true &&
        f.isDeleted === false
      }.map(r => (r.lockedBy, r.lockedDate))
        .update((Some(lock.by), Some(lock.date)))

      db.run(upd.transactionally).map { res =>
        if (res > 0) LockApplied(Option(lock))
        else LockError("Locking query did not match any documents")
      }
    }

  override def unlock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] =
    unlockManagedFile(fid) { dbId =>
      val upd = filesTable.filter { f =>
        f.id === dbId &&
        f.ownerId === ctx.owner.id.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === true
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

  override def editable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = {
    val query = editableQuery(from)

    db.run(query.result).map { rows =>
      if (rows.isEmpty) {
        log.debug(s"$from cannot be edited because query returned no results.")
        false
      } else {
        val res = rows.map(rowToManagedFile).forall(_.metadata.lock.isEmpty)
        log.debug(
          s"$from ${if (res) "can" else "cannot"} be edited because its" +
            s" ${if (res) "not in" else "in"} a locked folder tree."
        )
        res
      }
    }
  }

  override def markAsDeleted(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Either[String, Int]] = {
    val action = filesTable.filter { f =>
      f.fileId === fid &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.map(r => r.isDeleted).update(true)

    db.run(action.transactionally)
      .map {
        case res: Int if res > 0 =>
          Right(res)

        case res: Int if res == 0 =>
          val msg = s"Folder $fid was not marked as deleted"
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

}
