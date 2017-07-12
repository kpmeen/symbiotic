package net.scalytica.symbiotic.api.types

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class Folder(
    id: Option[UUID] = None,
    filename: String,
    metadata: ManagedMetadata
) extends ManagedFile {

  override val uploadDate: Option[DateTime] = None
  override val contentType: Option[String]  = None
  override val stream: Option[FileStream]   = None
  override val length: Option[String]       = None

  def flattenPath: Path = metadata.path.get

}

object Folder extends ManagedFileOps[Folder] {

  private val logger = LoggerFactory.getLogger(Folder.getClass)

  def apply(owner: UserId, path: Path): Folder = {
    val md = ManagedMetadata(
      owner = Some(owner),
      path = Some(path),
      isFolder = Some(true)
    )
    new Folder(
      filename = md.path.map(_.nameOfLast).getOrElse(Path.root.path),
      metadata = md
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
            metadata = mf.metadata
          )
        )

      case false =>
        None
    }

}
