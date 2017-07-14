package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}

case class TestOrgId(value: String) extends OrgId

object TestOrgId extends IdOps[TestOrgId] {
  override implicit def asId(s: String): TestOrgId = TestOrgId(s)
}

case class TestUserId(value: String) extends UserId

object TestUserId extends IdOps[TestUserId] {
  override implicit def asId(s: String): TestUserId = TestUserId(s)
}
