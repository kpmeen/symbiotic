package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types.SymbioticContext

case class TestContext(
    currentUser: TestUserId,
    owner: Owner,
    accessibleParties: Seq[PartyId]
) extends SymbioticContext {

  override def toOrgId(str: String): OrgId = TestOrgId.asId(str)

  override def toUserId(str: String): UserId = TestUserId.asId(str)

}
