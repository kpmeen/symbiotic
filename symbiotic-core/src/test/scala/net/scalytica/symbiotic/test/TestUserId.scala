package net.scalytica.symbiotic.test

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId

case class TestUserId(value: String) extends UserId

object TestUserId extends IdOps[TestUserId] {

  override implicit def asId(s: String): TestUserId = TestUserId(s)
}
