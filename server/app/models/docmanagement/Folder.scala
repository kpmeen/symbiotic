/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import models.docmanagement.MetadataKeys._
import models.party.PartyBaseTypes.OrganisationId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import repository.mongodb.DManFS

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

  val root = (oid: OrganisationId) => Folder(oid, Path.root)

}