package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.repository.FolderRepository
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.slf4j.LoggerFactory
import slick.dbio.DBIOAction

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
  ): Future[SaveResult[FileId]] = {
    exists(f).flatMap {
      case fe: Boolean if !fe =>
        log.debug(s"Folder at ${f.flattenPath} will be created.")
        val row = folderToRow(f)
        db.run(insertAction(row).transactionally).map(_ => Ok(row._2))

      case fe: Boolean if fe && Path.root != f.flattenPath =>
        log.debug(s"Folder at ${f.flattenPath} will be updated.")
        f.metadata.fid.map { fid =>
          db.run(updateAction(f).transactionally).map(_ => Ok(fid))
        }.getOrElse {
          Future.successful(
            InvalidData(s"Can't update folder because it's missing FolderId")
          )
        }

      case fe: Boolean if fe =>
        val msg = s"Folder at ${f.flattenPath} already exists."
        log.debug(msg)
        Future.successful(IllegalDestination(msg, f.metadata.path))
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying persist a folder.", ex)
        Failed(ex.getMessage)
    }
  }

  /*
    TODO: The current implementation is rather naive and just calls `get(fid)`.
    This won't be enough once folders supports a version scheme.
   */
  override def findLatestBy(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] = get(fid)

  override def get(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] = {
    val query = filesTable.filter { f =>
      f.fileId === folderId &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.result.headOption

    db.run(query)
      .map(_.map(r => Ok(rowToFolder(r))).getOrElse(NotFound()))
      .recover {
        case NonFatal(ex) =>
          log.error(s"An error occurred trying to get folder $folderId.", ex)
          Failed(ex.getMessage)
      }
  }

  override def get(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] = {
    val query = filesTable.filter { f =>
      f.path === at &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.result.headOption

    db.run(query)
      .map(_.map(r => Ok(rowToFolder(r))).getOrElse(NotFound()))
      .recover {
        case NonFatal(ex) =>
          log.error(s"An error occurred trying to get folder $at.", ex)
          Failed(ex.getMessage)
      }
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

    db.run(query).recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred checking if $at exists.", ex)
        throw ex
    }
  }

  override def filterMissing(p: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[List[Path]]] = {
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

    db.run(query.result)
      .map { res =>
        val folders = res.map(rowToFolder)
        Ok(allPaths.filterNot(p => folders.map(_.flattenPath).contains(p)))
      }
      .recover {
        case NonFatal(ex) =>
          log.error(s"An error fetching missing folders in $p.", ex)
          Failed(ex.getMessage)
      }
  }

  private[this] def moveBaseQry(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ) = {
    filesTable.filter { f =>
      f.ownerId === ctx.owner.id.value &&
      f.isDeleted === false &&
      accessiblePartiesFilter(f, ctx.accessibleParties)
    }
  }

  private[this] def childrenQry(orig: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ) = {
    moveBaseQry.filter(_.path startsWith orig)
  }

  private[this] def moveChildrenAction(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ) = {
    for {
      children <- childrenQry(orig).result.map(_.map(rowToManagedFile))
      updChildren <- DBIOAction.sequence(
                      children.map { mf =>
                        filesTable
                          .filter(_.fileId === mf.metadata.fid)
                          .map(_.path)
                          .update(mf.flattenPath.replaceParent(orig, mod))
                      }
                    )
    } yield updChildren.sum
  }

  override def move(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[MoveResult[Int]] = {
    val updateOrigQry = moveBaseQry
      .filter(f => f.path === orig && f.isFolder === true)
      .map(f => (f.fileName, f.path))
      .update((mod.nameOfLast, mod))

    val action = for {
      baseFolder <- updateOrigQry
      children   <- moveChildrenAction(orig, mod)
    } yield {
      baseFolder + children
    }

    db.run(action.transactionally)
      .map(r => if (r > 0) Ok(r) else NotModified())
      .recover {
        case NonFatal(ex) => Failed(ex.getMessage)
      }
      .recover {
        case NonFatal(ex) =>
          log.error(s"An error occurred trying move $orig to $mod.", ex)
          Failed(ex.getMessage)
      }
  }

  override def lock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockResult[Lock]] =
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
        if (res > 0) Ok(lock)
        else NotModified()
      }
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying lock folder $fid.", ex)
        Failed(ex.getMessage)
    }

  override def unlock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[UnlockResult[Unit]] =
    unlockManagedFile(fid) { dbId =>
      val upd = filesTable.filter { f =>
        f.id === dbId &&
        f.ownerId === ctx.owner.id.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isFolder === true
      }.map(r => (r.lockedBy, r.lockedDate)).update((None, None))

      db.run(upd.transactionally).map {
        case res: Int if res > 0 =>
          Ok(())

        case _ =>
          log.warn("Unlocking query did not match any documents")
          NotModified()
      }
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying unlock folder $fid.", ex)
        Failed(ex.getMessage)
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
  ): Future[DeleteResult[Int]] = {
    val action = filesTable.filter { f =>
      f.fileId === fid &&
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false
    }.map(r => r.isDeleted).update(true)

    db.run(action.transactionally)
      .map {
        case res: Int if res > 0  => Ok(res)
        case res: Int if res == 0 => NotModified()
      }
      .recover {
        case NonFatal(ex) =>
          log
            .error(s"An error occurred trying mark folder $fid as deleted.", ex)
          Failed(ex.getMessage)
      }
  }

}
