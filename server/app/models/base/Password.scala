/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import play.api.libs.json.{Reads, _}

case class Password(value: String)

object Password {

  lazy val empty = Password("")

  implicit val passwordReads: Reads[Password] =
    __.read[String].map(s => Password(s))
  implicit val passwordWrites: Writes[Password] = Writes { (e: Password) =>
    JsString(e.value)
  }

}
