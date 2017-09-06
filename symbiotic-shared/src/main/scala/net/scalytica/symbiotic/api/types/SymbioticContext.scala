package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties.Owner

trait SymbioticContext {

  /** Reference to the user currently interacting with the API's */
  val currentUser: UserId

  /**
   * Reference to the party that should own all resources being created by the
   * user in the folder sub-tree accessed by the user.
   */
  val owner: Owner

  /**
   * A list of party ID's identifying accessible data in the tree belonging to
   * the given Owner.
   *
   * This attribute is used to filter against the stored value in the metadata
   * field {{{accessibleBy}}} for {{{ManagedFiles}}}, used for restricting
   * access to sub-trees in a folder tree.
   */
  val accessibleParties: Seq[PartyId]

  def toOrgId(str: String): OrgId

  def toUserId(str: String): UserId

}
