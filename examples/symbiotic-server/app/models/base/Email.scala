package models.base

import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Contains and email string with appropriate validations etc.
 */
case class Email(adr: String) extends AnyVal

object Email {

  val NotProvided = "not_provided"

  val empty: Email = Email(NotProvided)

  implicit val emailReads: Reads[Email] =
    __.read[String](verifyingIf[String](_.trim.nonEmpty)(email))
      .map(Email.apply)

  implicit val emailWrites: Writes[Email] = Writes { e =>
    if (e == empty) JsNull
    else JsString(e.adr)
  }

  implicit def orderBy[A <: Email]: Ordering[A] = Ordering.by(ue => ue.adr)
}
