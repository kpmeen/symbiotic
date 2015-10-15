/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import core.mongodb.DManFS
import models.docmanagement.MetadataKeys._
import models.party.PartyBaseTypes.OrgId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class Folder(
  id: Option[FileId] = None,
  metadata: FileMetadata) extends BaseFile {

  override val filename: String = ""
  override val uploadDate: Option[DateTime] = None
  override val contentType: Option[String] = None

  def flattenPath: Path = metadata.path.get

}

object Folder extends DManFS {

  val logger = LoggerFactory.getLogger(Folder.getClass)

  def apply(oid: OrgId, path: Path) = new Folder(
    metadata = FileMetadata(
      oid = oid,
      path = Some(path),
      isFolder = Some(true)
    )
  )

  def rootFolder(oid: OrgId) = Folder(oid, Path.root)

  def fromBSON(dbo: DBObject): Folder = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    Folder(
      id = FileId.asMaybeId(mdbo._id),
      metadata = FileMetadata.fromBSON(md)
    )
  }

}