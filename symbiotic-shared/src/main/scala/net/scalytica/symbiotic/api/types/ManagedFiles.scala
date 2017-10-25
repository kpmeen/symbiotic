package net.scalytica.symbiotic.api.types

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * A ManagedFile is any file _or_ folder that is handled by the core engine.
 * Meaning, any file or folder that could be persisted with versioning and
 * metadata.
 */
trait ManagedFile extends SymbioticDocument[ManagedMetadata] {
  def flattenPath: Path = metadata.path.get
}

trait ManagedFileOps[A <: ManagedFile] {

  /**
   * Maps the ManagedFile arg to the type of A, which must be a sub-class of
   * ManagedFile.
   *
   * @param mf ManagedFile to map
   * @return An Option containing the ManagedFile as an instance of A.
   */
  def mapTo(mf: ManagedFile): Option[A]

}

/**
 * A {{{Folder}}} is a [[ManagedFile]] that represents a directory/folder/bucket
 * where files can be stored. A folder can contain any [[ManagedFile]] types.
 */
final case class Folder(
    id: Option[UUID] = None,
    filename: String,
    fileType: Option[String] = None,
    createdDate: Option[DateTime] = None,
    metadata: ManagedMetadata
) extends ManagedFile {

  override val stream: Option[FileStream] = None
  override val length: Option[String]     = None

}

object Folder extends ManagedFileOps[Folder] {

  private val logger = LoggerFactory.getLogger(Folder.getClass)

  def apply(owner: PartyId, path: Path): Folder = {
    Folder(
      filename = path.nameOfLast,
      metadata = ManagedMetadata(
        owner = Some(Owner(owner)),
        accessibleBy = Seq(AllowedParty(owner)),
        path = Some(path),
        isFolder = Some(true)
      )
    )
  }

  def apply(
      owner: PartyId,
      accessibleBy: Seq[AllowedParty],
      path: Path
  ): Folder = {
    Folder(
      filename = path.nameOfLast,
      metadata = ManagedMetadata(
        owner = Some(Owner(owner)),
        accessibleBy = AllowedParty(owner) +: accessibleBy,
        path = Some(path),
        isFolder = Some(true)
      )
    )
  }

  def apply(
      owner: PartyId,
      path: Path,
      tpe: Option[String],
      extraAttributes: Option[MetadataMap]
  ): Folder = {
    Folder(
      filename = path.nameOfLast,
      fileType = tpe,
      metadata = ManagedMetadata(
        owner = Some(Owner(owner)),
        accessibleBy = Seq(AllowedParty(owner)),
        path = Some(path),
        isFolder = Some(true),
        extraAttributes = extraAttributes
      )
    )
  }

  def apply(
      owner: PartyId,
      accessibleBy: Seq[AllowedParty],
      path: Path,
      tpe: Option[String],
      extraAttributes: Option[MetadataMap]
  ): Folder = {
    Folder(
      filename = path.nameOfLast,
      fileType = tpe,
      metadata = ManagedMetadata(
        owner = Some(Owner(owner)),
        accessibleBy = AllowedParty(owner) +: accessibleBy,
        path = Some(path),
        isFolder = Some(true),
        extraAttributes = extraAttributes
      )
    )
  }

  def root(ownerId: PartyId) = Folder(ownerId, Path.root)

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

/**
 * Represents a file to be up/down -loaded by a User.
 *
 * This is <i>NOT</i> a file in the sense of a java.util.File. But rather a
 * wrapper around an InputStream with quite a bit of extra Metadata information.
 */
final case class File(
    id: Option[UUID] = None,
    filename: String,
    fileType: Option[String] = None,
    createdDate: Option[DateTime] = None,
    length: Option[String] = None,
    stream: Option[FileStream] = None,
    metadata: ManagedMetadata
) extends ManagedFile

object File extends ManagedFileOps[File] {

  override def mapTo(mf: ManagedFile): Option[File] =
    mf.metadata.isFolder.flatMap {
      case true => None
      case false =>
        Option(
          File(
            id = mf.id,
            filename = mf.filename,
            fileType = mf.fileType,
            createdDate = mf.createdDate,
            length = mf.length,
            stream = mf.stream,
            metadata = mf.metadata
          )
        )
    }

}
