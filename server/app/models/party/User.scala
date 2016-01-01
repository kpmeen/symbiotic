/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import core.converters.DateTimeConverters
import models.base.PersistentType.VersionStamp
import models.base._
import models.party.PartyBaseTypes.{Party, UserId}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json.{Format, Json}

/**
 * Representation of a registered user in the system
 */
case class User(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  id: Option[UserId] = None,
  username: Username,
  email: Email,
  password: Password = Password.empty,
  name: Option[Name] = None,
  dateOfBirth: Option[DateTime] = None,
  gender: Option[Gender] = None,
  active: Boolean = true
) extends Party

object User extends PersistentTypeConverters with DateTimeConverters {

  val logger = LoggerFactory.getLogger(classOf[User])

  implicit val formats: Format[User] = Json.format[User]

}