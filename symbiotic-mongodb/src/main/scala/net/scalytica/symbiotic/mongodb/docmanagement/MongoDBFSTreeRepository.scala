package net.scalytica.symbiotic.mongodb.docmanagement

import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.FSTreeRepository
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.DManFS
// scalastyle:off
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits.managedfile_fromBSON
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

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def treeQuery(query: DBObject)(
      implicit f: DBObject => ManagedFile,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = Future {
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

    collection
      .aggregate(aggrQry)
      .results
      .map(mdbo => f(mdbo.as[DBObject]("doc")))
      .toSeq
  }

  override def treePaths(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[(FileId, Path)]] = Future {
    val query = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      IsFolderKey.full $eq true,
      PathKey.full $eq Path.regex(from.getOrElse(Path.root))
    )
    val fields = MongoDBObject(FidKey.full -> 1, PathKey.full -> 1)

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
  }

  override def tree(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = {
    val query = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      PathKey.full $eq Path.regex(from.getOrElse(Path.root))
    )
    treeQuery(query)
  }

  override def children(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = {
    val f = from.getOrElse(Path.root)
    treeQuery(
      $and(
        OwnerIdKey.full $eq ctx.owner.id.value,
        AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
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
