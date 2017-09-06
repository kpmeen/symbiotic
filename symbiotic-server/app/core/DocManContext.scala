package core

import models.base.{SymbioticOrgId, SymbioticUserId}
import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types.SymbioticContext

case class DocManContext(
    currentUser: SymbioticUserId,
    owner: Owner,
    accessibleParties: Seq[PartyId]
) extends SymbioticContext {

  override def toOrgId(str: String) = SymbioticOrgId.asId(str)

  override def toUserId(str: String) = SymbioticUserId.asId(str)

}

object DocManContext {

  def apply(currentUser: SymbioticUserId): DocManContext = {
    DocManContext(currentUser, Owner(currentUser), Seq(currentUser))
  }

}
