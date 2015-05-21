/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.converters

import models.base.{DBId, Id}
import org.bson.types.ObjectId
import play.api.libs.json._

/**
 * This helps for transforming type specific Id's to/from JSON. Each companion Id implementations companion object
 * should look something like this:
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
trait WithIdConverters[A <: Id] {

  implicit def writes: Writes[A] = Writes {
    (a: A) => JsString(a.value)
  }

  implicit def reads(t: (String) => A): Reads[A] = __.read[String].map(o => t(o))

  implicit def asId(s: String): A

  implicit def asOptIdString(maybeId: String): Option[A] = Option(maybeId).map(s => asId(s))

  def create(): A = asId(java.util.UUID.randomUUID.toString)
}

trait WithDBIdConverters[A <: DBId] extends WithIdConverters[A] {

  implicit def asId(oid: ObjectId): A = asId(oid.toString)

  implicit def asOptId(maybeId: Option[ObjectId]): Option[A] = maybeId.map(oid => asId(oid))

  override def create(): A = asId(new ObjectId())
}


