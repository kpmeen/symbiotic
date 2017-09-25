package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.FSTreeRepository
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PostgresFSTreeRepository(
    val config: Config
) extends FSTreeRepository
    with SymbioticDb
    with SymbioticDbTables
    with SharedQueries {

  private val logger = LoggerFactory.getLogger(this.getClass)

  import profile.api._

  logger.debug(s"Initialized repository $getClass")

  def treeQuery(
      query: Query[FileTable, FileTable#TableElementType, Seq]
  )(implicit ec: ExecutionContext): Future[Seq[ManagedFile]] = {
    val q = query
      .sortBy(_.fileName.asc)
      .sortBy(_.path.asc)
      .sortBy(_.version.desc)
      .sortBy(_.isFolder.desc)
      .result

    db.run(q).map(_.map(rowToManagedFile))
  }

  override def treePaths(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[(FileId, Path)]] = {
    val query = filesTable.filter { f =>
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false &&
      (f.path startsWith from.getOrElse(Path.root))
    }.sortBy(f => f.path.asc).map(f => f.fileId -> f.path)

    db.run(query.result).map(_.map(row => row._1 -> row._2))
  }

  override def tree(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = {
    val q1 = filesTable.filter { f =>
      f.ownerId === ctx.owner.id.value &&
      f.isDeleted === false &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      (f.path regexMatch Path.regex(from.getOrElse(Path.root)))
    }
    val q2 = filesTable.groupBy(_.fileId).map {
      case (id, row) => (id, row.map(_.version).max)
    }

    val query = for {
      f1 <- q1
      f2 <- q2
      if f1.fileId === f2._1
    } yield f1

    treeQuery(query)
  }

  override def children(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = {
    val fromRegex =
      Path.regex(from.getOrElse(Path.root), subFoldersOnly = true)

    val query =
      filesTable.filter { f =>
        f.ownerId === ctx.owner.id.value &&
        accessiblePartiesFilter(f, ctx.accessibleParties) &&
        f.isDeleted === false &&
        (
          (f.isFolder === false && f.path === from) ||
          (f.isFolder === true && (f.path regexMatch fromRegex))
        )
      }
    treeQuery(query)
  }

}
