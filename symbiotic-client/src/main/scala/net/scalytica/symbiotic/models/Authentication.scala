package net.scalytica.symbiotic.models

import play.api.libs.json.{Format, Json}

case class Credentials(uname: String, pass: String)

object Credentials {
  implicit val format: Format[Credentials] = Json.format[Credentials]
}

case class AuthToken(token: String)

object AuthToken {
  implicit val format: Format[AuthToken] = Json.format[AuthToken]
}

case class LoginInfo(providerID: String, providerKey: String)

object LoginInfo {
  implicit val format: Format[LoginInfo] = Json.format[LoginInfo]

  val empty = LoginInfo("", "")

  val credentialsProvider: String = "credentials"
}
