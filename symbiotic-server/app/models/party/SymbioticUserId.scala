package models.party

import net.scalytica.symbiotic.data.IdOps
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.play.json.IdFormatters
import play.api.libs.json.Format

case class SymbioticUserId(value: String) extends UserId

object SymbioticUserId
    extends IdOps[SymbioticUserId]
    with IdFormatters[SymbioticUserId] {

  implicit val format: Format[SymbioticUserId] =
    Format(reads(SymbioticUserId.apply), writes)

  override implicit def asId(s: String): SymbioticUserId = SymbioticUserId(s)
}
