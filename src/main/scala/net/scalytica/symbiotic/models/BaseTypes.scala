/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models

sealed trait PartyId

case class UserId(value: String)

case class OrgId(value: String)

case class Name(first: Option[String], middle: Option[String], last: Option[String]) {
  def print: String = s"${first.getOrElse("")} ${middle.getOrElse("")} ${last.getOrElse("")}".trim()
}

case class UserStamp(date: String, by: String)

case class VersionStamp(
  version: Int = 1,
  created: Option[UserStamp] = None,
  modified: Option[UserStamp] = None)
