package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types.SymbioticContext

case class TestContext(
    currentUser: TestUserId,
    owner: Owner,
    accessibleParties: Seq[PartyId]
) extends SymbioticContext {

  override def toOrgId(str: String) = TestOrgId.asId(str)

  override def toUserId(str: String) = TestUserId.asId(str)

}
