/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.docmanagement

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import core.mongodb.DManFS
import models.docmanagement.MetadataKeys._
import models.docmanagement.Path
import models.party.PartyBaseTypes.OrgId
import org.slf4j.LoggerFactory

object FSTree extends DManFS {

  val logger = LoggerFactory.getLogger(FSTree.getClass)

  /**
   * Allows for composition of a tree structure.
   */
  def tree[A](oid: OrgId, query: DBObject, fields: Option[DBObject])(f: DBObject => A): Seq[A] = {
    val res = fields.fold(collection.find(query))(collection.find(query, _))
    res.sort(MongoDBObject(IsFolderKey.full -> -1, "filename" -> 1, PathKey.full -> 1)).map(mdbo => f(mdbo)).toSeq
  }

  /**
   * Fetch only the Paths for the full folder tree structure, without any file refs.
   *
   * @param oid OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treePaths(oid: OrgId, from: Path = Path.root): Seq[Path] = {
    val query = MongoDBObject(OidKey.full -> oid.value, IsFolderKey.full -> true, PathKey.full -> Path.regex(from))
    val fields = Option(MongoDBObject(PathKey.full -> 1))

    tree(oid, query, fields) { mdbo =>
      mdbo.getAs[DBObject](MetadataKey).flatMap(dbo => dbo.getAs[String](PathKey.key).map(Path.apply))
    }.filter(_.isDefined).map(_.get)
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
  def treeWith[A](oid: OrgId, from: Path = Path.root)(f: (MongoDBObject) => A): Seq[A] = {
    val query = MongoDBObject(OidKey.full -> oid.value, PathKey.full -> Path.regex(from))
    tree(oid, query, None)(mdbo => f(mdbo))
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
  def childrenWith[A](oid: OrgId, from: Path = Path.root)(f: (MongoDBObject) => A): Seq[A] = {
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
    ), None)(mdbo => f(mdbo))
  }
}
