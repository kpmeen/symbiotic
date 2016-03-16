/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models

case class Credentials(uname: String, pass: String)

case class AuthToken(token: String)

case class LoginInfo(providerID: String, providerKey: String)

object LoginInfo {
  val empty = LoginInfo("", "")

  val credentialsProvider: String = "credentials"
}