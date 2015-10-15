/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import java.util.Date

import com.mongodb.casbah.Imports._
import core.converters.{DateTimeConverters, ObjectBSONConverters}
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
  active: Boolean = true) extends Party

object User extends PersistentTypeConverters with DateTimeConverters with ObjectBSONConverters[User] {

  val logger = LoggerFactory.getLogger(classOf[User])

  implicit val formats: Format[User] = Json.format[User]

  /**
   * Converts a User instance to BSON format
   */
  implicit override def toBSON(u: User): DBObject = {
    val builder = MongoDBObject.newBuilder
    u._id.foreach(builder += "_id" -> _)
    u.v.foreach(builder += "v" -> VersionStamp.toBSON(_))
    u.id.foreach(builder += "id" -> _.value)
    builder += "username" -> u.username.value
    builder += "email" -> u.email.adr
    builder += "password" -> u.password.value
    u.name.foreach(n => builder += "name" -> Name.toBSON(n))
    u.dateOfBirth.foreach(d => builder += "dateOfBirth" -> d.toDate)
    u.gender.foreach(g => builder += "gender" -> g.value)
    builder += "active" -> u.active

    builder.result()
  }

  /**
   * Converts a BSON document to an instance of User
   */
  implicit override def fromBSON(d: DBObject): User = {
    User(
      _id = d.getAs[ObjectId]("_id"),
      v = d.getAs[DBObject]("v").map(VersionStamp.fromBSON),
      id = d.getAs[String]("id"),
      username = Username(d.as[String]("username")),
      email = Email(d.as[String]("email")),
      password = d.getAs[String]("password").map(Password.apply).getOrElse(Password.empty),
      name = d.getAs[DBObject]("name").flatMap(n => Option(Name.fromBSON(n))),
      dateOfBirth = asOptDateTime(d.getAs[Date]("dateOfBirth")),
      gender = d.getAs[String]("gender").flatMap(g => Gender.fromString(g)),
      active = d.getAs[Boolean]("active").getOrElse(true)
    )
  }

}