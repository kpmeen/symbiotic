/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.converters

import models.base.Id
import play.api.libs.json._

/**
 * This helps for transforming type specific Id's to/from JSON. Each companion
 * Id implementations companion object should look something like this:
 *
 * {{{
 *   object TheType extends WithIdTransformers {
 *     implicit val theReads = reads[TheType](TheType.apply)
 *     implicit val theWrites = writes[TheType]
 *     ...
 *   }
 * }}}
 *
 */
trait IdConverters[A <: Id] {

  implicit def writes: Writes[A] = Writes {
    (a: A) => JsString(a.value)
  }

  implicit def reads(t: (String) => A): Reads[A] = __.read[String].map(o => t(o))

  implicit def asId(s: String): A

  implicit def asOptId(maybeId: Option[String]): Option[A] = maybeId.map(s => asId(s))

  implicit def asOptId(s: String): Option[A] = asOptId(Option(s))

  def create(): A = asId(java.util.UUID.randomUUID.toString)

  def createOpt(): Option[A] = asOptId(java.util.UUID.randomUUID.toString)
}

