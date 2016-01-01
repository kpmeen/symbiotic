/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import core.converters.DateTimeConverters
import models.base.PersistentType.VersionStamp
import models.party.PartyBaseTypes.UserId
import org.bson.types.ObjectId
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
  implicit val oidReads: Reads[ObjectId] = __.read[String].map(s => new ObjectId(s))
  implicit val oidWrites: Writes[ObjectId] = Writes {
    (a: ObjectId) => JsString(a.toString)
  }
}
