/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Contains and email string with appropriate validations etc.
 */
case class Email(adr: String)

object Email {
  implicit val emailReads: Reads[Email] = __.read[String](verifyingIf[String](_.trim.nonEmpty)(email)).map(Email.apply)

  implicit val emailWrites: Writes[Email] = Writes {
    (e: Email) => JsString(e.adr)
  }

  implicit def orderByValue[A <: Email]: Ordering[A] = Ordering.by(ue => ue.adr)
}
