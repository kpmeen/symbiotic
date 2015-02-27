/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Username(value: String)

object Username {

  implicit val usernameReads: Reads[Username] =
    __.read[String](
      verifyingIf[String](_.trim.nonEmpty)(minLength[String](3) keepAnd maxLength[String](20))
    ).map(Username.apply)

  implicit val usernameWrites: Writes[Username] = Writes {
    (e: Username) => JsString(e.value)
  }

}
