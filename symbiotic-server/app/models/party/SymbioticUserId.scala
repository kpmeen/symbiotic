package models.party

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.UserIdOps
import net.scalytica.symbiotic.play.json.IdFormatters
import play.api.libs.json.{Format, Reads}

case class SymbioticUserId(value: String) extends UserId

object SymbioticUserId
    extends UserIdOps[SymbioticUserId]
    with IdFormatters[UserId] {

  implicit val reads: Reads[UserId] =
    Format(reads(SymbioticUserId.apply), writes)

  override implicit def asId(s: String): SymbioticUserId = SymbioticUserId(s)
}
