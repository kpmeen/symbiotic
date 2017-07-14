package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner

import scala.concurrent.Future

trait SymbioticContext {

  /** Reference to the user currently interacting with the API's */
  val currentUser: UserId

  /**
   * Reference to the party that owns the filesystem hierarchy the current user
   * is interacting with.
   */
  val owner: Owner

  def canAccess: Future[Boolean]

  def toOrgId(str: String): OrgId

  def toUserId(str: String): UserId

}
