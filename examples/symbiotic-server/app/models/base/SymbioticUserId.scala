package models.base

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import play.api.libs.json._

case class SymbioticUserId(value: String) extends UserId

object SymbioticUserId extends IdOps[SymbioticUserId] {

  implicit val reads: Reads[SymbioticUserId] = Reads {
    _.validate[String] match {
      case JsSuccess(value, _) => JsSuccess(asId(value))
      case err: JsError        => err
    }
  }

  implicit val writes: Writes[SymbioticUserId] =
    Writes[UserId](uid => JsString(uid.value))

  override implicit def asId(s: String): SymbioticUserId = SymbioticUserId(s)

}
