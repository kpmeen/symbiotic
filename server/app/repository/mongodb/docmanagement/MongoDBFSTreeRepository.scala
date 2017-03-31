/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.docmanagement

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.Imports._
import models.docmanagement.MetadataKeys._
import models.docmanagement.{FileId, ManagedFile, Path}
import models.party.PartyBaseTypes.UserId
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.FSTreeRepository
import repository.mongodb.DManFS
import repository.mongodb.bson.BSONConverters.Implicits.managedfile_fromBSON

/**
 * General queries into the Folder and File hierarchy of GridFS.
 * Typical use cases includes fetching the full folder tree with or without
 * content, all the children (files/folders) of a given Folder, etc...
 */
@Singleton
class MongoDBFSTreeRepository @Inject()(
    val configuration: Configuration
) extends FSTreeRepository
    with DManFS {

  val logger = LoggerFactory.getLogger(this.getClass)

  def treeQuery(
      query: DBObject
  )(implicit f: DBObject => ManagedFile): Seq[ManagedFile] = {
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
            "fname"  -> "$filename",
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

  override def treePaths(
      from: Option[Path]
  )(implicit uid: UserId): Seq[(FileId, Path)] = {
    val query = MongoDBObject(
      OwnerKey.full    -> uid.value,
      IsFolderKey.full -> true,
      PathKey.full     -> Path.regex(from.getOrElse(Path.root))
    )
    val fields = MongoDBObject(FidKey.full -> 1, PathKey.full -> 1)

    collection
      .find(query, fields)
      .sort(MongoDBObject(PathKey.full -> 1))
      .map { mdbo =>
        mdbo
          .getAs[MongoDBObject](MetadataKey)
          .map(
            dbo =>
              (
                FileId(dbo.as[String](FidKey.key)),
                Path(dbo.as[String](PathKey.key))
            )
          )
      }
      .toSeq
      .filter(_.isDefined)
      .flatten
  }

  override def tree(
      from: Option[Path]
  )(implicit uid: UserId): Seq[ManagedFile] = {
    val query = MongoDBObject(
      OwnerKey.full -> uid.value,
      PathKey.full  -> Path.regex(from.getOrElse(Path.root))
    )
    treeQuery(query)
  }

  override def children(
      from: Option[Path]
  )(implicit uid: UserId): Seq[ManagedFile] = {
    val f = from.getOrElse(Path.root)
    treeQuery(
      $and(
        OwnerKey.full $eq uid.value,
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
