/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.docmanagement

import com.mongodb.casbah.Imports._
import models.docmanagement.CommandStatusTypes.{CommandError, CommandKo, CommandOk, CommandStatus}
import models.docmanagement.MetadataKeys._
import models.docmanagement._
import models.party.PartyBaseTypes.OrganisationId
import org.slf4j.LoggerFactory

import scala.util.Try

object FolderService extends ManagedFileService {

  val logger = LoggerFactory.getLogger(FolderService.getClass)

  /**
   * Checks for the existence of a Folder
   *
   * @param f Folder
   * @return true if the folder exists, else false
   */
  def exists(f: Folder): Boolean = exists(f.metadata.oid, f.flattenPath)

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param oid OrgId
   * @param at Path to look for
   * @return true if the folder exists, else false
   */
  def exists(oid: OrganisationId, at: Path): Boolean =
    collection.findOne(MongoDBObject(
      OidKey.full -> oid.value,
      PathKey.full -> at.materialize,
      IsFolderKey.full -> true
    )).isDefined

  /**
   * Will attempt to identify if any path segments in the provided folders path is missing.
   * If found, a list of the missing Folders will be returned.
   *
   * @param oid OrgId
   * @param p Path
   * @return list of missing folders
   */
  def filterMissing(oid: OrganisationId, p: Path): List[Path] = {

    case class CurrPathMiss(path: String, missing: List[Path])

    val segments = p.path.split("/").filterNot(_.isEmpty)

    // Left fold over the path segments and identify the ones that doesn't exist
    segments.foldLeft[CurrPathMiss](CurrPathMiss("", List.empty))((prev: CurrPathMiss, seg: String) => {
      val p = if (prev.path.isEmpty) seg else s"${prev.path}/$seg"
      val next = Path(p)
      if (exists(oid, next)) CurrPathMiss(p, prev.missing)
      else CurrPathMiss(p, next +: prev.missing)
    }).missing
  }

  /**
   * Create a new virtual folder in GridFS.
   * If the folder is not defined, the method will attempt to create a root folder if it does not already exist.
   *
   * @param f the folder to add
   * @return An option containing the Id of the created folder, or none if it already exists
   */
  def save(f: Folder): Option[FileId] = {
    if (!exists(f)) {
      val fid = Some(f.metadata.fid.getOrElse(FileId.create()))
      val sd = MongoDBObject(
        "filename" -> f.filename,
        MetadataKey -> ManagedFileMetadata.toBSON(f.metadata.copy(fid = fid))
      )
      Try {
        logger.debug(s"Creating folder $f")
        collection.save(sd)
        fid
      }.recover {
        case e: Throwable =>
          logger.error(s"An error occurred trying to save $f", e)
          None
      }.get
    } else {
      None
    }
  }

  /**
   * This method allows for modifying the path from one value to another.
   * Should only be used in conjunction with the appropriate checks for any child nodes.
   *
   * @param oid OrgId
   * @param orig FolderPath
   * @param mod FolderPath
   * @return Option of Int with number of documents affected by the update
   */
  def move(oid: OrganisationId, orig: Path, mod: Path): CommandStatus[Int] = {
    val qry = MongoDBObject(
      OidKey.full -> oid.value,
      PathKey.full -> orig.materialize
    )
    val upd = $set("filename" -> mod.nameOfLast, PathKey.full -> mod.materialize)

    Try {
      val res = collection.update(qry, upd)
      if (res.getN > 0) CommandOk(res.getN)
      else CommandKo(0)
    }.recover {
      case e: Throwable => CommandError(0, Option(e.getMessage))
    }.get
  }

}
