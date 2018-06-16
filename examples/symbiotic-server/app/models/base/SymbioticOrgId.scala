package models.base

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.OrgId
import play.api.libs.json._

case class SymbioticOrgId(value: String) extends OrgId

object SymbioticOrgId extends IdOps[SymbioticOrgId] {

  implicit val reads: Reads[SymbioticOrgId] = Reads {
    _.validate[String] match {
      case JsSuccess(value, _) => JsSuccess(asId(value))
      case err: JsError        => err
    }
  }

  implicit val writes: Writes[SymbioticOrgId] =
    Writes[OrgId](oid => JsString(oid.value))

  override implicit def asId(s: String): SymbioticOrgId = SymbioticOrgId(s)
}
