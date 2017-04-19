package models.base

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.play.json.IdFormat
import play.api.libs.json.{Reads, Writes}

case class SymbioticUserId(value: String) extends UserId

object SymbioticUserId
    extends IdOps[SymbioticUserId]
    with IdFormat[SymbioticUserId] {

  implicit val reads: Reads[SymbioticUserId]   = Reads(jsv => reads(jsv))
  implicit val writes: Writes[SymbioticUserId] = Writes(uid => writes(uid))

  override implicit def asId(s: String): SymbioticUserId = SymbioticUserId(s)
}
