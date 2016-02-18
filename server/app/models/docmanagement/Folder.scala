/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import java.util.UUID

import models.party.PartyBaseTypes.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import repository.mongodb.DManFS

case class Folder(
    id: Option[UUID] = None,
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

  def apply(owner: UserId, path: Path) = new Folder(
    metadata = ManagedFileMetadata(
      owner = Some(owner),
      path = Some(path),
      isFolder = Some(true)
    )
  )

  def root(owner: UserId) = Folder(owner, Path.root)

}