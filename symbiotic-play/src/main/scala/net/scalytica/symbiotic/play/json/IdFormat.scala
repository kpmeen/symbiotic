package net.scalytica.symbiotic.play.json

import net.scalytica.symbiotic.api.types.{Id, IdOps}
import play.api.libs.json._

/**
 * This helps for transforming type specific Id's to/from JSON.
 *
 * {{{
 *   object TheType extends IdFormat[TheIdType] {
 *     implicit val f: Format[TheIdType] = Format(reads(TheIdType.apply, writes))
 *   }
 * }}}
 *
 */
trait IdFormat[A <: Id] extends IdWrites[A] with IdReads[A]

trait IdWrites[A <: Id] extends Writes[A] with IdOps[A] {

  override implicit def writes(a: A): JsValue = JsString(a.value)

}

trait IdReads[A <: Id] extends Reads[A] with IdOps[A] {
  override implicit def reads(jsv: JsValue): JsResult[A] =
    jsv.validate[String] match {
      case JsSuccess(value, jsPath) => JsSuccess(asId(value))
      case err: JsError             => err
    }
}
