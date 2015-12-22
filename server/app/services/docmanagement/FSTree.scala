/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.docmanagement

import com.mongodb.casbah.Imports._
import core.mongodb.DManFS
import models.docmanagement.MetadataKeys._
import models.docmanagement.Path
import models.party.PartyBaseTypes.OrganisationId
import org.slf4j.LoggerFactory

object FSTree extends DManFS {

  val logger = LoggerFactory.getLogger(FSTree.getClass)

  /**
   * Performs an aggregation query to build a file/folder-tree structure that only
   * contains the latest version of each file.
   */
  def tree[A](oid: OrganisationId, query: DBObject)(f: DBObject => A): Seq[A] = {
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

  /**
   * Fetch only the Paths for the full folder tree structure, without any file refs.
   *
   * @param oid OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treePaths(oid: OrganisationId, from: Path = Path.root): Seq[Path] = {
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

  /**
   * This method will return the a collection of A instances , representing the folder/directory
   * structure that has been set-up in GridFS.
   *
   * @param oid OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @param f Function for converting a MongoDBObject to types of A
   * @return a collection of A instances
   */
  def treeWith[A](oid: OrganisationId, from: Path = Path.root)(f: (MongoDBObject) => A): Seq[A] = {
    val query = MongoDBObject(OidKey.full -> oid.value, PathKey.full -> Path.regex(from))
    tree(oid, query)(mdbo => f(mdbo))
  }

  /**
   * This method will return the a collection of A instances , representing the direct descendants
   * for the given Folder.
   *
   * @param oid OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @param f Function for converting a MongoDBObject to types of A
   * @return a collection of A instances
   */
  def childrenWith[A](oid: OrganisationId, from: Path = Path.root)(f: (MongoDBObject) => A): Seq[A] = {
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
