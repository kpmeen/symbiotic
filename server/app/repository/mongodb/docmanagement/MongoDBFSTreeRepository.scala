/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.docmanagement

import com.mongodb.casbah.Imports._
import models.docmanagement.MetadataKeys._
import models.docmanagement.Path
import models.party.PartyBaseTypes.OrganisationId
import org.slf4j.LoggerFactory
import repository.FSTreeRepository
import repository.mongodb.DManFS

/**
 * General queries into the Folder and File hierarchy of GridFS.
 * Typical use cases includes fetching the full folder tree with or without content, all the children
 * (files/folders) of a given Folder, etc...
 */
object MongoDBFSTreeRepository extends FSTreeRepository[DBObject, DBObject] with DManFS {

  val logger = LoggerFactory.getLogger(MongoDBFSTreeRepository.getClass)

  override def tree[A](oid: OrganisationId, query: DBObject)(f: DBObject => A): Seq[A] = {
    val aggrQry = List(
      MongoDBObject("$match" -> query),
      MongoDBObject("$sort" -> MongoDBObject(
        PathKey.full -> 1,
        VersionKey.full -> -1
      )),
      MongoDBObject("$group" -> MongoDBObject(
        "_id" -> MongoDBObject(
          "fname" -> "$filename",
          "folder" -> "$metadata.isFolder"
        ),
        "doc" -> MongoDBObject("$first" -> "$$ROOT")
      )),
      MongoDBObject("$sort" -> MongoDBObject(
        s"doc.${IsFolderKey.full}" -> -1,
        s"doc.${PathKey.full}" -> 1
      ))
    )

    collection.aggregate(aggrQry).results.map(mdbo => f(mdbo.as[DBObject]("doc"))).toSeq
  }

  override def treePaths(oid: OrganisationId, from: Path = Path.root): Seq[Path] = {
    val query = MongoDBObject(
      OidKey.full -> oid.value,
      IsFolderKey.full -> true,
      PathKey.full -> Path.regex(from)
    )
    val fields = MongoDBObject(PathKey.full -> 1)

    collection.find(query, fields).sort(MongoDBObject(PathKey.full -> 1)).map { mdbo =>
      mdbo.getAs[MongoDBObject](MetadataKey).flatMap(dbo => dbo.getAs[String](PathKey.key).map(Path.apply))
    }.toSeq.filter(_.isDefined).map(_.get)
  }

  override def treeWith[A](oid: OrganisationId, from: Path = Path.root)(f: (DBObject) => A): Seq[A] = {
    val query = MongoDBObject(OidKey.full -> oid.value, PathKey.full -> Path.regex(from))
    tree(oid, query)(mdbo => f(mdbo))
  }

  override def childrenWith[A](oid: OrganisationId, from: Path = Path.root)(f: (DBObject) => A): Seq[A] = {
    tree(oid, $and(
      OidKey.full $eq oid.value,
      $or(
        $and(
          IsFolderKey.full $eq false,
          PathKey.full $eq from.materialize
        ),
        $and(
          IsFolderKey.full $eq true,
          PathKey.full $eq Path.regex(from, subFoldersOnly = true)
        )
      )
    ))(mdbo => f(mdbo))
  }
}
