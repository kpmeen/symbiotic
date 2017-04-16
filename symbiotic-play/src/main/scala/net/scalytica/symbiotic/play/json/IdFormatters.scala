package net.scalytica.symbiotic.play.json

import net.scalytica.symbiotic.api.types.{FileId, Id}
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

  implicit def reads(t: (String) => A): Reads[A] =
    __.read[String].map(o => t(o))
}

object FileIdFormat extends IdFormatters[FileId] {
  implicit val f: Format[FileId] = Format(reads(FileId.apply), writes)
}

trait IdImplicits {

  implicit val fileIdFormat: Format[FileId] = implicitly(FileIdFormat.f)

}
