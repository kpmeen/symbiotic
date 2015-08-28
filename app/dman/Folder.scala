/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import com.mongodb.casbah.Imports._
import core.mongodb.DManFS
import dman.FolderPath._
import dman.MetadataKeys._
import models.customer.CustomerId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.util.Try

case class Folder(
  id: Option[FileId] = None,
  filename: String,
  metadata: FileMetadata) extends BaseFile {

  override val uploadDate: Option[DateTime] = None
  override val contentType: Option[String] = None

  def flattenPath: FolderPath = metadata.path.get

}

object Folder extends DManFS {

  val logger = LoggerFactory.getLogger(Folder.getClass)

  /**
   * Checks for the existence of a Folder
   *
   * @param f Folder
   * @return true if the folder exists, else false
   */
  def exists(f: Folder): Boolean = FolderPath.exists(f.metadata.cid, f.flattenPath)

  /**
   * Create a new virtual folder in GridFS.
   * If the folder is not defined, the method will attempt to create a root folder if it does not already exist.
   *
   * @param f the folder to add
   * @return An option containing the Id of the created folder, or none if it already exists
   */
  def save(f: Folder): Option[FolderId] = {
    if (!exists(f)) {
      val sd = MongoDBObject(MetadataKey -> MongoDBObject(
        CidKey.key -> f.metadata.cid.value,
        PathKey.key -> f.flattenPath.materialize,
        IsFolderKey.key -> true
      ))
      Try {
        collection.save(sd)
        sd.getAs[ObjectId]("_id")
      }.recover {
        case e: Throwable =>
          logger.error(s"An error occurred trying to save $f", e)
          None
      }.get
    } else {
      None
    }
  }

}