package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types.SymbioticContext

case class TestContext(
    currentUser: TestUserId,
    owner: Owner
) extends SymbioticContext {

  override def canAccess = ???

  override def toOrgId(str: String) = TestOrgId.asId(str)

  override def toUserId(str: String) = TestUserId.asId(str)

}
