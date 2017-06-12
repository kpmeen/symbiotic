package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.UserIdOps

case class TestUserId(value: String) extends UserId

object TestUserId extends UserIdOps[TestUserId] {

  override implicit def asId(s: String): TestUserId = TestUserId(s)
}
