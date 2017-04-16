package net.scalytica.symbiotic

import net.scalytica.symbiotic.data.IdOps
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId

case class TestUserId(value: String) extends UserId

object TestUserId extends IdOps[TestUserId] {

  override implicit def asId(s: String): TestUserId = TestUserId(s)
}
