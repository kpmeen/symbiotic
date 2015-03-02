/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import java.util.Date

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.{WithBSONConverters, WithMongo}
import models.base._
import models.base.mapping.WithDateTimeMapping
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json

/**
 * Representation of a registered user in the system
 */
case class User(
  id: Option[UserId] = None,
  username: Username,
  email: Email,
  password: Password,
  name: Option[Name] = None,
  dateOfBirth: Option[DateTime] = None,
  gender: Option[Gender] = None) extends Individual

object User extends WithDateTimeMapping with WithMongo with WithBSONConverters[User] {

  val logger = Logger(classOf[User])

  val collectionName = "users"

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  /**
   * Converts a User instance to BSON format
   */
  override implicit def toBSON(u: User): DBObject = {
    val builder = MongoDBObject.newBuilder
    u.id.foreach(builder += "_id" -> _.id)
    builder += "username" -> u.username
    builder += "email" -> u.email.adr
    builder += "password" -> u.password.value
    u.name.foreach(n => builder += "name" -> Name.toBSON(n))
    u.dateOfBirth.foreach(d => builder += "dateOfBirth" -> d.toDate)
    u.gender.foreach(g => builder += "gender" -> g.value)

    builder.result()
  }

  /**
   * Converts a BSON document to an instance of User
   */
  override implicit def fromBSON(d: DBObject): User = {
    User(
      id = Option(UserId(d.as[ObjectId]("_id"))),
      username = d.as[Username]("username"),
      email = Email(d.as[String]("email")),
      password = Password(d.as[String]("password")),
      name = d.getAs[DBObject]("name").flatMap(n => Option(Name.fromBSON(n))),
      dateOfBirth = d.getAs[Date]("dateOfBirth").flatMap(jd => Option(new DateTime(jd.getTime))),
      gender = d.getAs[String]("gender").flatMap(g => Gender.fromString(g))
    )
  }

  /**
   * This service will save a User instance to MongoDB. Basically it is performing an upsert. Meaning that a new
   * document will be inserted if the User doesn't exist. Otherwise the existing entry will be updated.
   */
  def save(usr: User) = {
    val res = collection.save(usr)

    if (res.isUpdateOfExisting) logger.info("Updated existing user")
    else logger.info("Inserted new user")

    logger.info(res.toString)
  }

  /**
   * Find the user with given userId
   */
  def findById(userId: UserId): Option[User] = {
    collection.findOneByID(userId.id).flatMap(uct => Option(uct))
  }

  /**
   * Find the user with the given username
   */
  def findByUsername(username: Username): Option[User] = {
    collection.findOne(MongoDBObject("username" -> username.value)).flatMap(uct => Option(uct))
  }
}