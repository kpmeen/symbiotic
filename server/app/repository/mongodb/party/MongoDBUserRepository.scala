/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.party

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mongodb.casbah.Imports._
import core.lib._
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.User
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.UserRepository
import repository.mongodb.bson.BSONConverters.Implicits._
import repository.mongodb.{DefaultDB, WithMongoIndex}

import scala.util.Try

@Singleton
class MongoDBUserRepository @Inject() (
    val configuration: Configuration
) extends UserRepository with DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(this.getClass)

  override val collectionName = "users"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("username", unique = true),
    Indexable("email"),
    Indexable("loginInfo")
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
  override def findById(userId: UserId): Option[User] =
    collection.findOne(MongoDBObject("_id" -> userId.value))
      .map(uct => user_fromBSON(uct))

  /**
   * Find the user with the given username
   */
  override def findByUsername(username: Username): Option[User] =
    collection.findOne(MongoDBObject("username" -> username.value))
      .map(uct => user_fromBSON(uct))

  override def findByLoginInfo(loginInfo: LoginInfo): Option[User] =
    collection.findOne(MongoDBObject("loginInfo" -> loginInfo_toBSON(loginInfo)))
      .map(uct => user_fromBSON(uct))
}
