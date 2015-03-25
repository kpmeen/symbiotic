/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.docmanagement

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import core.docmanagement.CommandStatusTypes._
import core.docmanagement.DocumentManagement._
import core.docmanagement.MetadataKeys._
import core.mongodb.WithGridFS
import models.customer.CustomerId
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try
import scala.util.matching.Regex

/**
 * Simulates a folder (directory) in a file system.
 *
 * Folder paths are built up using materialized paths pattern in MongoDB
 * (See http://docs.mongodb.org/manual/tutorial/model-tree-structures-with-materialized-paths)
 *
 * Basically each file will be stored with a path. This path is relevant to the location of the file.
 * The path is stored as a , (comma) separated String. Each customer gets 1 base folder called ,root,.
 */
case class Folder(var path: String = "root") {

  path = path.replaceAll(",", "/")

  /**
   * Converts the path value into a comma separated (materialized) String for persistence.
   */
  def materialize: String = {
    val x = if (!path.startsWith("/")) s"/$path" else path
    val y = if (!x.endsWith("/")) s"$x/" else x
    val z = if (!y.startsWith("/root")) s"/root$y" else y
    z.replaceAll("/", ",")
  }

}

// TODO: Validation on path value syntax (, (comma) separated)
object Folder extends WithGridFS {

  val logger = LoggerFactory.getLogger(Folder.getClass)

  implicit val folderReads: Reads[Folder] = __.readNullable[String].map(fromDisplay)
  implicit val folderWrites: Writes[Folder] = Writes {
    case f: Folder => JsString(Folder.toDisplay(f))
  }

  val rootFolder: Folder = Folder("root")

  /**
   * Converter to map between a DBObject (from read operations) to a Folder.
   * This will typically be used when listing folders in a GridFS bucket.
   *
   * @param dbo DBObject
   * @return Folder
   */
  def fromDBObject(dbo: DBObject): Folder = {
    dbo.getAs[DBObject](MetadataKey).flatMap(dbo =>
      dbo.getAs[String](PathKey.key).map(Folder.apply)
    ).getOrElse(rootFolder)
  }

  def regex(p: Folder): Regex = s"^${p.materialize}".r

  private def toDisplay(p: Folder): String = Option(p.path).getOrElse("/")

  private def fromDisplay(s: Option[String]): Folder = s.map(Folder.apply).getOrElse(rootFolder)

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param cid CustomerId
   * @param at Path to look for
   * @return true if the folder exists, else false
   */
  def exists(cid: CustomerId, at: Folder): Boolean =
    collection.findOne(MongoDBObject(MetadataKey -> MongoDBObject(
      CidKey.key -> cid.id,
      PathKey.key -> at.materialize,
      IsFolderKey.key -> true
    ))).isDefined

  /**
   * Create a new virtual folder in GridFS for provided customerId at given folder.
   * If the folder is not defined, the method will attempt to create a root folder if it does not already exist.
   *
   * @param cid CustomerId
   * @param at the path location of the folder to add or none if root folder should be created.
   * @return An option containing the Id of the created folder, or none if it already exists
   */
  def save(cid: CustomerId, at: Folder = Folder.rootFolder): Option[FolderId] = {
    if (!folderExists(cid, at)) {
      val sd = MongoDBObject(MetadataKey -> MongoDBObject(
        CidKey.key -> cid.id,
        PathKey.key -> at.materialize,
        IsFolderKey.key -> true
      ))
      Try {
        collection.save(sd)
        sd.getAs[ObjectId]("_id")
      }.recover {
        case e: Throwable =>
          logger.error(s"An error occurred trying to save $at", e)
          None
      }.get
    } else {
      None
    }
  }

  /**
   * This method allows for modifiying the path from one value to another.
   * Should only be used in conjunction with the appropriate checks for any child nodes.
   *
   * @param cid CustomerId
   * @param orig Folder
   * @param mod Folder
   * @return Option of Int with number of documents affected by the update
   */
  def updatePath(cid: CustomerId, orig: Folder, mod: Folder): CommandStatus[Int] = {
    val qry = MongoDBObject(CidKey.full -> cid.id, PathKey.full -> orig.materialize)
    val upd = $set(PathKey.full -> mod.materialize)

    Try {
      val res = collection.update(qry, upd)
      if (res.getN > 0) CommandOk(res.getN)
      else CommandKo(0)
    }.recover {
      case e: Throwable => CommandError(0, Option(e.getMessage))
    }.get
  }

  /**
   * Intended for pushing vast amounts of folders into gridfs...
   *
   * TODO: Move to test sources (?)
   */
  private[docmanagement] def bulkInsert(cid: CustomerId, fl: List[Folder]): Unit = {
    val toAdd = fl.map(f => MongoDBObject(MetadataKey -> MongoDBObject(
      CidKey.key -> cid.id,
      PathKey.key -> f.materialize,
      IsFolderKey.key -> true
    )))
    Try(collection.insert(toAdd: _*)).recover {
      case e: Throwable => logger.error(s"An error occurred inserting a bulk of folders", e)
    }
  }

  /**
   * Will attempt to identify if any path segments in the provided folders path is missing.
   * If found, a list of the missing Folders will be returned.
   *
   * @param cid CustomerId
   * @param f Folder
   * @return list of missing folders
   */
  def filterMissing(cid: CustomerId, f: Folder): List[Folder] = {

    case class CurrPathMiss(path: String, missing: List[Folder])

    val segments = f.path.split("/").filterNot(_.isEmpty)
    // Left fold over the path segments and identify the ones that doesn't exist
    segments.foldLeft[CurrPathMiss](CurrPathMiss("", List.empty))((prev: CurrPathMiss, seg: String) => {
      val p = if (prev.path.isEmpty) seg else s"${prev.path}/$seg"
      val next = Folder(p)
      if (folderExists(cid, next)) {
        logger.debug(s"folder exists: $p")
        CurrPathMiss(p, prev.missing)
      } else {
        logger.debug(s"folder didn't exist: $p")
        CurrPathMiss(p, next +: prev.missing)
      }
    }).missing
  }

  /**
   * Allows for composition of a tree structure.
   */
  def tree[A](cid: CustomerId, from: Folder = Folder.rootFolder, query: DBObject, fields: Option[DBObject])(f: DBObject => A): Seq[A] = {
    val res = fields.fold(collection.find(query))(collection.find(query, _))
    res.sort(MongoDBObject(PathKey.full -> 1)).map(mdbo => f(mdbo)).toSeq
  }

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param cid CustomerId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treeNoFiles(cid: CustomerId, from: Folder = Folder.rootFolder): Seq[Folder] = {
    val query = MongoDBObject(CidKey.full -> cid.id, IsFolderKey.full -> true, PathKey.full -> regex(from))
    val fields = Option(MongoDBObject(PathKey.full -> 1))

    tree[Option[Folder]](cid, from, query, fields)(mdbo =>
      mdbo.getAs[DBObject](MetadataKey).flatMap(dbo => dbo.getAs[String](PathKey.key).map(Folder.apply))
    ).filter(_.isDefined).map(_.get)
  }

  /**
   * This method will return the a collection of A instances , representing the folder/directory
   * structure that has been set-up in GridFS.
   *
   * @param cid CustomerId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @param f Function for converting a MongoDBObject to types of A
   * @return a collection of A instances
   */
  def treeWith[A](cid: CustomerId, from: Folder = Folder.rootFolder)(f: (MongoDBObject) => A): Seq[A] = {
    val query = MongoDBObject(CidKey.full -> cid.id) ++ MongoDBObject(PathKey.full -> regex(from))
    tree(cid, from, query, None)(mdbo => f(mdbo))
  }

}