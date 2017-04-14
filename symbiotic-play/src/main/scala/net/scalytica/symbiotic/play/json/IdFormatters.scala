package net.scalytica.symbiotic.play.json

import net.scalytica.symbiotic.data.{FileId, FolderId, Id}
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

object FileIdFormat extends IdFormatters[FileId] {
  implicit val f: Format[FolderId] = Format(reads(FileId.apply), writes)
}

trait IdImplicits {

  implicit val userIdFormat: Format[UserId]   = implicitly(UserIdFormat.f)
  implicit val fileIdFormat: Format[FolderId] = implicitly(FileIdFormat.f)

}
