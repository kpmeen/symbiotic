package net.scalytica.symbiotic.api.types

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class Folder(
    id: Option[UUID] = None,
    filename: String,
    fileType: Option[String] = None,
    metadata: ManagedMetadata
) extends ManagedFile {

  override val uploadDate: Option[DateTime] = None
  override val stream: Option[FileStream]   = None
  override val length: Option[String]       = None

  def flattenPath: Path = metadata.path.get

}

object Folder extends ManagedFileOps[Folder] {

  private val logger = LoggerFactory.getLogger(Folder.getClass)

  def apply(owner: UserId, path: Path): Folder = {
    Folder(
      filename = path.nameOfLast,
      metadata = ManagedMetadata(
        owner = Some(owner),
        path = Some(path),
        isFolder = Some(true)
      )
    )
  }

  def apply(
      owner: UserId,
      path: Path,
      tpe: Option[String],
      extraAttributes: Option[MetadataMap]
  ): Folder = {
    Folder(
      filename = path.nameOfLast,
      fileType = tpe,
      metadata = ManagedMetadata(
        owner = Some(owner),
        path = Some(path),
        isFolder = Some(true),
        extraAttributes = extraAttributes
      )
    )
  }

  def root(owner: UserId) = Folder(owner, Path.root)

  override def mapTo(mf: ManagedFile): Option[Folder] =
    mf.metadata.isFolder.flatMap {
      case true =>
        Option(
          Folder(
            id = mf.id,
            filename = mf.filename,
            fileType = mf.fileType,
            metadata = mf.metadata
          )
        )

      case false =>
        None
    }

}
