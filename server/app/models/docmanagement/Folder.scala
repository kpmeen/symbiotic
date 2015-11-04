/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import core.mongodb.DManFS
import models.docmanagement.MetadataKeys._
import models.party.PartyBaseTypes.OrganisationId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class Folder(
    id: Option[ObjectId] = None,
    metadata: ManagedFileMetadata
) extends ManagedFile {

  override val filename: String = metadata.path.map(_.nameOfLast).getOrElse(Path.root.path)
  override val uploadDate: Option[DateTime] = None
  override val contentType: Option[String] = None
  override val stream: Option[FileStream] = None
  override val length: Option[String] = None

  def flattenPath: Path = metadata.path.get

}

object Folder extends DManFS {

  val logger = LoggerFactory.getLogger(Folder.getClass)

  def apply(oid: OrganisationId, path: Path) = new Folder(
    metadata = ManagedFileMetadata(
      oid = oid,
      path = Some(path),
      isFolder = Some(true)
    )
  )

  def rootFolder(oid: OrganisationId) = Folder(oid, Path.root)

  def fromBSON(dbo: DBObject): Folder = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    Folder(
      id = mdbo._id,
      metadata = ManagedFileMetadata.fromBSON(md)
    )
  }

}