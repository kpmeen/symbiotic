package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FolderRepository
import net.scalytica.symbiotic.api.types.CommandStatusTypes._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PostgresFolderRepository(
    val config: Config
) extends FolderRepository
    with SymbioticDb
    with SymbioticDbTables {

  private val logger =
    LoggerFactory.getLogger(classOf[PostgresFolderRepository])

  import profile.api._

  logger.debug(s"Initialized repository $getClass")

  private def insertAction(row: FileRow) = {
    (filesTable returning filesTable.map(_.id)) += row
  }

  private def updateAction(f: Folder) = {
    filesTable
      .filter(_.id === f.id)
      .map { row =>
        (row.version, row.contentType, row.description, row.customMetadata)
      }
      .update(
        (
          f.metadata.version,
          f.fileType,
          f.metadata.description,
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
        logger.debug(s"Folder at ${f.flattenPath} will be created.")
        val row = folderToRow(f)
        db.run(insertAction(row)).map(_ => Option(row._2))

      case fe: Boolean if fe && Path.root != f.flattenPath =>
        logger.debug(s"Folder at ${f.flattenPath} will be updated.")
        db.run(updateAction(f)).map(_ => f.metadata.fid)

      case fe: Boolean if fe =>
        logger.debug(s"Folder at ${f.flattenPath} already exists.")
        Future.successful(None)
    }
  }

  override def get(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = {
    val query = filesTable.filter { f =>
      f.fileId === folderId &&
      f.ownerId === ctx.owner.id.value &&
      f.isFolder === true
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
      f.isFolder === true
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
      f.isFolder === true
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
      f.isFolder === true
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
    val action = filesTable
      .filter(f => f.ownerId === ctx.owner.id.value && f.path === orig)
      .map(f => (f.fileName, f.path))
      .update((mod.nameOfLast, mod))

    db.run(action)
      .map(r => if (r > 0) CommandOk(r) else CommandKo(0))
      .recover {
        case NonFatal(ex) =>
          CommandError(0, Option(ex.getMessage))
      }
  }

}
