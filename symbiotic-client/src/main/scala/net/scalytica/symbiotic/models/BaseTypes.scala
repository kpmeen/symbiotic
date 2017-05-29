/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models

import java.util.UUID

trait Id {

  val value: String

  def toUUID: UUID = UUID.fromString(value)

}

case class UserId(value: String) extends Id

case class FileId(value: String) extends Id

object FileId {
  lazy val empty = FileId("")

  implicit def fromString(str: String): FileId = FileId(str)
}

case class Name(
    first: Option[String] = None,
    middle: Option[String] = None,
    last: Option[String] = None
) {
  def print: String =
    s"${first.getOrElse("")} ${middle.getOrElse("")} ${last.getOrElse("")}"
      .trim()
}

case class UserStamp(date: String, by: String)

case class VersionStamp(
    version: Int = 1,
    created: Option[UserStamp] = None,
    modified: Option[UserStamp] = None
)
