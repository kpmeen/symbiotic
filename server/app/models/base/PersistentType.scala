/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import java.util.UUID

import core.converters.DateTimeConverters
import models.base.PersistentType.VersionStamp
import models.party.PartyBaseTypes.UserId
import org.joda.time.DateTime
import play.api.libs.json._

trait PersistentType {
  val v: Option[VersionStamp]
}

object PersistentType {

  case class UserStamp(date: DateTime, by: UserId)

  object UserStamp extends DateTimeConverters {
    implicit val msFormat: Format[UserStamp] = Json.format[UserStamp]

    def create(uid: UserId): UserStamp = UserStamp(DateTime.now, uid)
  }

  case class VersionStamp(
    version: Int = 1,
    created: Option[UserStamp] = None,
    modified: Option[UserStamp] = None
  )

  object VersionStamp {
    implicit val vsFormat: Format[VersionStamp] = Json.format[VersionStamp]
  }

}

trait PersistentTypeConverters {
  implicit val oidReads: Reads[UUID] = __.read[String].map(s => UUID.fromString(s))
  implicit val oidWrites: Writes[UUID] = Writes {
    (a: UUID) => JsString(a.toString)
  }
}
