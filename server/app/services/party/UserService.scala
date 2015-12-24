/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.mongodb.casbah.Imports._
import core.lib._
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.User
import org.slf4j.LoggerFactory

import scala.util.Try

// TODO: This is really a repository implementation...refactor once the repo interfaces have been defined!
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
   */
  def save(usr: User): SuccessOrFailure = {
    Try {
      val res = collection.save(usr)
      logger.debug(res.toString)

      if (res.isUpdateOfExisting) Updated
      else Created
    }.recover {
      case t =>
        logger.warn(s"An error occurred when saving $usr", t)
        throw t
    }.getOrElse {
      Failure(s"User $usr could not be saved")
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
