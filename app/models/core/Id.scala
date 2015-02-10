/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import com.mongodb.casbah.TypeImports.ObjectId
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Base trait defining an Id throughout the system. All type specific Id's should extend this trait
 */
trait Id {
  val id: ObjectId
}

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
trait WithIdTransformers {

  implicit def writes[A <: Id]: Writes[A] = Writes {
    (a: A) => JsString(a.id.toString)
  }

  implicit def reads[A <: Id](t: (ObjectId) => A): Reads[A] = __.read[String].map(o => t(new ObjectId(o)))
}



