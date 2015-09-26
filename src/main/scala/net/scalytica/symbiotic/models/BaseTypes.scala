/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models

import scala.scalajs.js

sealed trait PartyId

case class UserId(value: String)

case class OrgId(value: String)

case class Name(first: Option[String], middle: Option[String], last: Option[String])

case class Email(adr: String)

case class Username(value: String)

case class Password(value: String)

object Password {
  lazy val empty = Password("")
}

case class UserStamp(date: js.Date, by: String)

case class VersionStamp(
  version: Int = 1,
  created: Option[UserStamp] = None,
  modified: Option[UserStamp] = None)
