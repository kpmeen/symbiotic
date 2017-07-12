package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId

/**
 * The base representation of available metadata fields throughout the system
 */
trait BaseMetadata

/**
 * Extension point to implement custom metadata
 */
trait BaseManagedMetadata extends BaseMetadata {
  val owner: Option[UserId]
  val fid: Option[FileId]
  val uploadedBy: Option[UserId]
  val version: Version
  val isFolder: Option[Boolean]
  val path: Option[Path]
  val description: Option[String]
  val lock: Option[Lock]
  val extraAttributes: Option[MetadataMap]
}

/**
 * The actual implementation of the metadata representation used through the
 * lower levels of the internal APIs.
 */
case class ManagedMetadata(
    owner: Option[UserId] = None,
    fid: Option[FileId] = None,
    uploadedBy: Option[UserId] = None,
    version: Version = 1,
    isFolder: Option[Boolean] = None,
    path: Option[Path] = None,
    description: Option[String] = None,
    lock: Option[Lock] = None,
    extraAttributes: Option[MetadataMap] = None
) extends BaseManagedMetadata
