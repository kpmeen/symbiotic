package net.scalytica.symbiotic.mongodb.docmanagement

import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.SymbioticResults.{Failed, GetResult, Ok}
import net.scalytica.symbiotic.api.repository.FSTreeRepository
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.DManFS

import scala.util.control.NonFatal
// scalastyle:off
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits.managedfileFromBSON
// scalastyle:on
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 * General queries into the Folder and File hierarchy of GridFS.
 * Typical use cases includes fetching the full folder tree with or without
 * content, all the children (files/folders) of a given Folder, etc...
 */
class MongoDBFSTreeRepository(
    val configuration: Config
) extends FSTreeRepository
    with DManFS {

  private val log = LoggerFactory.getLogger(this.getClass)

  private def treeQuery(query: DBObject)(
      implicit f: DBObject => ManagedFile,
      ec: ExecutionContext
  ): Future[GetResult[Seq[ManagedFile]]] =
    Future {
      val aggrOpts = AggregationOptions(AggregationOptions.CURSOR)
      val aggrQry = List(
        MongoDBObject("$match" -> query),
        MongoDBObject(
          "$sort" -> MongoDBObject(
            PathKey.full    -> 1,
            VersionKey.full -> -1
          )
        ),
        MongoDBObject(
          "$group" -> MongoDBObject(
            "_id" -> MongoDBObject(
              "fname"  -> "$metadata.fid",
              "folder" -> "$metadata.isFolder"
            ),
            "doc" -> MongoDBObject("$first" -> "$$ROOT")
          )
        ),
        MongoDBObject(
          "$sort" -> MongoDBObject(
            s"doc.${IsFolderKey.full}" -> -1,
            s"doc.${PathKey.full}"     -> 1
          )
        )
      )

      val files = collection
        .aggregate(aggrQry, aggrOpts)
        .map(mdbo => f(mdbo.as[DBObject]("doc")))
        .toSeq

      Ok(files)
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to fetch tree.", ex)
        Failed(ex.getMessage)
    }

  override def treePaths(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[(FileId, Path)]]] =
    Future {
      val query = $and(
        OwnerIdKey.full $eq ctx.owner.id.value,
        AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
        IsFolderKey.full $eq true,
        IsDeletedKey.full $eq false,
        PathKey.full $eq Path.regex(from.getOrElse(Path.root))
      )
      val fields = MongoDBObject(FidKey.full -> 1, PathKey.full -> 1)

      Ok(
        collection
          .find(query, fields)
          .sort(MongoDBObject(PathKey.full -> 1))
          .map { mdbo =>
            mdbo.getAs[MongoDBObject](MetadataKey).map { dbo =>
              (
                FileId(dbo.as[String](FidKey.key)),
                Path(dbo.as[String](PathKey.key))
              )
            }
          }
          .toSeq
          .filter(_.isDefined)
          .flatten
      )
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to paths in tree.", ex)
        Failed(ex.getMessage)
    }

  override def tree(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[ManagedFile]]] = {
    val query = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      IsDeletedKey.full $eq false,
      PathKey.full $eq Path.regex(from.getOrElse(Path.root))
    )
    treeQuery(query)
  }

  override def children(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[ManagedFile]]] = {
    val f = from.getOrElse(Path.root)
    treeQuery(
      $and(
        OwnerIdKey.full $eq ctx.owner.id.value,
        AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
        IsDeletedKey.full $eq false,
        $or(
          $and(
            IsFolderKey.full $eq false,
            PathKey.full $eq f.materialize
          ),
          $and(
            IsFolderKey.full $eq true,
            PathKey.full $eq Path.regex(f, subFoldersOnly = true)
          )
        )
      )
    )
  }

}
