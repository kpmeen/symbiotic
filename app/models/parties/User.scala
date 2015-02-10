/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import java.util.Date

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import models.core._
import models.core.mapping.WithDateTimeMapping
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import services.WithMongo

/**
 * Representation of a registered user in the system
 */
case class User(
  id: Option[UserId] = None,
  username: String,
  email: Email,
  password: Password,
  name: Option[Name] = None,
  dateOfBirth: Option[DateTime] = None,
  gender: Option[Gender] = None) extends Individual

object User extends WithDateTimeMapping with WithMongo {

  val logger = Logger(classOf[User])

  val collectionName = "users"

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  def toBSON(usr: User): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "_id" -> usr.id.map(_.id).getOrElse(new ObjectId)
    builder += "username" -> usr.username
    builder += "email" -> usr.email.adr
    builder += "password" -> usr.password.value
    usr.name.foreach(n => builder += "name" -> Name.toBSON(n))
    usr.dateOfBirth.foreach(d => builder += "dateOfBirth" -> d.toDate)
    usr.gender.foreach(g => builder += "gender" -> g.value)

    builder.result()
  }

  def fromBSON(dbo: DBObject): User = {
    User(
      id = Option(UserId(dbo.as[ObjectId]("_id"))),
      username = dbo.as[String]("username"),
      email = Email(dbo.as[String]("email")),
      password = Password(dbo.as[String]("password")),
      name = dbo.getAs[DBObject]("name").flatMap(n => Option(Name.fromBSON(n))),
      dateOfBirth = dbo.getAs[Date]("dateOfBirth").flatMap(d => Option(new DateTime(d.getTime))),
      gender = dbo.getAs[String]("gender").flatMap(g => Gender.fromString(g))
    )
  }

  def save(usr: User) = {
    val res = collection.save(User.toBSON(usr))

    if (res.isUpdateOfExisting) logger.info("Updated existing user")
    else logger.info("Inserted new user")

    logger.info(res.toString)
  }

  def findById(userId: UserId): Option[User] = {
    collection.findOneByID(userId.id).flatMap(uct => Option(fromBSON(uct)))
  }

  def findByUsername(username: String): Option[User] = {
    collection.findOne(MongoDBObject("username" -> username)).flatMap(uct => Option(fromBSON(uct)))
  }

}