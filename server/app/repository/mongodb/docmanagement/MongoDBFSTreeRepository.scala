/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.docmanagement

import com.google.inject.Singleton
import com.mongodb.casbah.Imports._
import models.docmanagement.MetadataKeys._
import models.docmanagement.{ManagedFile, Path}
import models.party.PartyBaseTypes.OrganisationId
import org.slf4j.LoggerFactory
import repository.FSTreeRepository
import repository.mongodb.DManFS
import repository.mongodb.bson.BSONConverters.Implicits.managedfile_fromBSON

/**
 * General queries into the Folder and File hierarchy of GridFS.
 * Typical use cases includes fetching the full folder tree with or without content, all the children
 * (files/folders) of a given Folder, etc...
 */
@Singleton
class MongoDBFSTreeRepository extends FSTreeRepository with DManFS {

  val logger = LoggerFactory.getLogger(this.getClass)

  def treeQuery(oid: OrganisationId, query: DBObject)(implicit f: DBObject => ManagedFile): Seq[ManagedFile] = {
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

  override def tree(oid: OrganisationId, from: Path = Path.root): Seq[ManagedFile] = {
    val query = MongoDBObject(OidKey.full -> oid.value, PathKey.full -> Path.regex(from))
    treeQuery(oid, query)
  }

  override def children(oid: OrganisationId, from: Path = Path.root): Seq[ManagedFile] = {
    treeQuery(oid, $and(
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
    ))
  }
}
