package models.base

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Username(value: String) extends AnyVal

object Username {

  val MinLength = 3
  val MaxLength = 20

  private[this] def isValid(s: String): Boolean =
    s.length >= Username.MinLength && s.length <= Username.MaxLength

  def fromString(s: String): Option[Username] =
    if (isValid(s)) Some(Username(s)) else None

  implicit val usernameReads: Reads[Username] =
    __.read[String](
        verifyingIf[String](_.trim.nonEmpty)(
          minLength[String](MinLength) keepAnd maxLength[String](MaxLength)
        )
      )
      .map(Username.apply)

  implicit val usernameWrites: Writes[Username] = Writes(e => JsString(e.value))

}
