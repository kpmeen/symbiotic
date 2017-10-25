package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{PartyId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}

/**
 * The base representation of available metadata fields throughout the system
 */
trait BaseMetadata

/**
 * Extension point to implement custom metadata
 */
trait BaseManagedMetadata extends BaseMetadata {
  val owner: Option[Owner]
  val accessibleBy: Seq[AllowedParty]
  val fid: Option[FileId]
  val createdBy: Option[UserId]
  val version: Version
  val isFolder: Boolean
  val isDeleted: Boolean
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
    owner: Option[Owner] = None,
    accessibleBy: Seq[AllowedParty] = Seq.empty,
    fid: Option[FileId] = None,
    createdBy: Option[UserId] = None,
    version: Version = 1,
    isFolder: Boolean = true,
    isDeleted: Boolean = false,
    path: Option[Path] = None,
    description: Option[String] = None,
    lock: Option[Lock] = None,
    extraAttributes: Option[MetadataMap] = None
) extends BaseManagedMetadata {

  def grantAccess(partyId: PartyId): ManagedMetadata = {
    if (accessibleBy.contains(partyId)) this
    else copy(accessibleBy = AllowedParty(partyId) +: accessibleBy)
  }

  def revokeAccess(partyId: PartyId): ManagedMetadata = {
    if (accessibleBy.contains(partyId))
      copy(accessibleBy = accessibleBy.filterNot(_.id == partyId))
    else
      this
  }

}
