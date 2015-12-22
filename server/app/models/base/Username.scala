/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Username(value: String) extends AnyVal

object Username {

  private val MinLength = 3
  private val MaxLength = 20

  implicit val usernameReads: Reads[Username] =
    __.read[String](
      verifyingIf[String](_.trim.nonEmpty)(minLength[String](MinLength) keepAnd maxLength[String](MaxLength))
    ).map(Username.apply)

  implicit val usernameWrites: Writes[Username] = Writes {
    (e: Username) => JsString(e.value)
  }

}
