/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import java.util.Date

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.PersistentType.VersionStamp
import models.base._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

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
  active: Boolean = true) extends Individual

object User extends PersistentTypeConverters with DateTimeConverters with DefaultDB with WithMongoIndex with ObjectBSONConverters[User] {

  val logger = LoggerFactory.getLogger(classOf[User])

  val collectionName = "users"

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  // TODO... this should _really_ be done in the UserService...once implemented!
  ensureIndex()

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
  override def fromBSON(d: DBObject): User = {
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

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("username", unique = true)
  ), collection)

  /**
   * This service will save a User instance to MongoDB. Basically it is performing an upsert. Meaning that a new
   * document will be inserted if the User doesn't exist. Otherwise the existing entry will be updated.
   *
   * TODO: return a proper indication of whether the user was added or updated.
   */
  def save(usr: User) = {
    val res = collection.save(usr)

    if (res.isUpdateOfExisting) logger.info("Updated existing user")
    else logger.info("Inserted new user")

    logger.debug(res.toString)
  }

  /**
   * Find the user with given userId
   */
  def findById(userId: UserId): Option[User] = {
    collection.findOne(MongoDBObject("id" -> userId.value)).map(uct => fromBSON(uct))
  }

  /**
   * Find the user with the given username
   */
  def findByUsername(username: Username): Option[User] = {
    collection.findOne(MongoDBObject("username" -> username.value)).map(uct => fromBSON(uct))
  }
}