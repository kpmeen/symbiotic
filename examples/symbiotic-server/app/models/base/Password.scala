package models.base

import play.api.libs.json.{Reads, _}

case class Password(value: String)

object Password {

  lazy val empty = Password("")

  implicit val passwordReads: Reads[Password] =
    __.read[String].map(Password.apply)
  implicit val passwordWrites: Writes[Password] = Writes { e =>
    JsString(e.value)
  }

}
