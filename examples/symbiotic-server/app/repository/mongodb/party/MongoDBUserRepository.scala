package repository.mongodb.party

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mongodb.casbah.Imports._
import models.base.{SymbioticUserId, Username}
import models.party.User
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.mongodb.{DefaultDB, WithMongoIndex}
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.UserRepository
import repository.mongodb.bson.UserProfileBSONConverters.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class MongoDBUserRepository @Inject()(config: Configuration)
    extends UserRepository
    with DefaultDB
    with WithMongoIndex {

  override def configuration = config.underlying

  val log = LoggerFactory.getLogger(this.getClass)

  override val collectionName = "users"

  ensureIndex()

  override def ensureIndex(): Unit =
    index(
      List(
        Indexable("username", unique = true),
        Indexable("email"),
        Indexable("loginInfo")
      ),
      collection
    )

  /**
   * This service will save a User instance to MongoDB. Basically it is
   * performing an upsert. Meaning that a new document will be inserted if the
   * User doesn't exist. Otherwise the existing entry will be updated.
   */
  override def save(
      usr: User
  )(implicit ec: ExecutionContext): Future[Either[String, SymbioticUserId]] =
    Future {
      Try {
        val uid  = usr.id.getOrElse(SymbioticUserId.create())
        val user = usr.copy(id = Some(uid))

        val res = collection.save(user)
        log.debug(res.toString)

        if (res.isUpdateOfExisting) {
          log.debug(s"Successfully updated ${usr.username}")
        }

        if (0 < res.getN) Right(uid)
        else Left(s"User ${usr.username} was not saved.")
      }.recover {
        case t =>
          log.warn(s"An error occurred when saving $usr", t)
          throw t
      }.getOrElse {
        Left(s"User $usr could not be saved")
      }
    }

  /**
   * Find the user with given userId
   */
  override def findById(
      userId: UserId
  )(implicit ec: ExecutionContext): Future[Option[User]] = Future {
    collection
      .findOne(MongoDBObject("_id" -> userId.value))
      .map(uct => user_fromBSON(uct))
  }

  /**
   * Find the user with the given username
   */
  override def findByUsername(
      username: Username
  )(implicit ec: ExecutionContext): Future[Option[User]] = Future {
    collection
      .findOne(MongoDBObject("username" -> username.value))
      .map(uct => user_fromBSON(uct))
  }

  override def findByLoginInfo(
      loginInfo: LoginInfo
  )(implicit ec: ExecutionContext): Future[Option[User]] = Future {
    collection
      .findOne(MongoDBObject("loginInfo" -> loginInfo_toBSON(loginInfo)))
      .map(uct => user_fromBSON(uct))
  }
}
