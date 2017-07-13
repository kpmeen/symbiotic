package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FolderRepository
import net.scalytica.symbiotic.api.types.CommandStatusTypes._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
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
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    exists(f).flatMap { folderExists =>
      val (fid, action) =
        if (!folderExists) {
          val row = folderToRow(f)
          Option(row._2) -> insertAction(row)
        } else {
          f.metadata.fid -> updateAction(f)
        }

      db.run(action).map(_ => fid)
    }
  }

  override def get(folderId: FolderId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[Folder]] = {
    val query = filesTable.filter { f =>
      f.fileId === folderId && f.owner === uid && f.isFolder === true
    }.result.headOption

    db.run(query).map(_.map(rowToFolder))
  }

  override def get(at: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[Folder]] = {
    val query = filesTable.filter { f =>
      f.path === at && f.owner === uid && f.isFolder === true
    }.result.headOption

    db.run(query).map(_.map(rowToFolder))
  }

  override def exists(at: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Boolean] = {
    val query = filesTable.filter { f =>
      f.path === at && f.owner === uid && f.isFolder === true
    }.exists.result
    db.run(query)
  }

  override def filterMissing(p: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[List[Path]] = {
    val allPaths =
      p.path.split("/").filterNot(_.isEmpty).foldLeft(List.empty[Path]) {
        case (paths, curr) =>
          paths :+ paths.lastOption
            .map(l => Path(s"${l.path}/$curr"))
            .getOrElse(Path.root)
      }

    val query = filesTable.filter { f =>
      (f.path inSet allPaths) &&
      f.owner === uid &&
      f.isFolder === true
    }

    db.run(query.result).map { res =>
      val folders = res.map(rowToFolder)
      allPaths.filterNot(p => folders.map(_.flattenPath).contains(p))
    }
  }

  override def move(orig: Path, mod: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[CommandStatus[Int]] = {
    val action = filesTable
      .filter(f => f.owner === uid && f.path === orig)
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
