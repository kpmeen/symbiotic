/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package controllers.converters

import net.scalytica.symbiotic.data.Id
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import play.api.libs.json._

/**
 * This helps for transforming type specific Id's to/from JSON.
 *
 * {{{
 *   object TheType extends IdFormatters[TheIdType] {
 *     implicit val f: Format[TheIdType] = Format(reads(TheIdType.apply, writes))
 *   }
 * }}}
 *
 */
trait IdFormatters[A <: Id] {

  implicit def writes: Writes[A] = Writes((a: A) => JsString(a.value))

  // format: off
  implicit def reads(t: (String) => A): Reads[A] = __.read[String].map(o => t(o)) // scalastyle:ignore
  // format: on
}

object UserIdFormat extends IdFormatters[UserId] {
  implicit val f: Format[UserId] = Format(reads(UserId.apply), writes)
}

object Implicits {

  implicit val userIdFormat = implicitly(UserIdFormat.f)

}
