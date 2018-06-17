package models.base

import play.api.libs.json.{Reads, _}

case class Password(value: String)

object Password {

  lazy val empty: Password = Password("")

  implicit val pwdReads: Reads[Password]   = __.read[String].map(Password.apply)
  implicit val pwdWrites: Writes[Password] = Writes(e => JsString(e.value))

}
