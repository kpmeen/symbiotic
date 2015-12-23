/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.mongodb.casbah.Imports._
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.User
import org.slf4j.LoggerFactory

import scala.util.Try

object UserService extends DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(UserService.getClass)

  override val collectionName = "users"

  ensureIndex()

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
  def save(usr: User): Unit = {
    Try {
      val res = collection.save(usr)

      if (res.isUpdateOfExisting) logger.info("Updated existing user")
      else logger.info("Inserted new user")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"User could not be saved", t)
    }
  }

  /**
   * Find the user with given userId
   */
  def findById(userId: UserId): Option[User] = {
    collection.findOne(MongoDBObject("id" -> userId.value)).map(uct => User.fromBSON(uct))
  }

  /**
   * Find the user with the given username
   */
  def findByUsername(username: Username): Option[User] = {
    collection.findOne(MongoDBObject("username" -> username.value)).map(uct => User.fromBSON(uct))
  }
}
