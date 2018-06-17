package repository.mongodb.silhouette

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mongodb.MongoException
import com.mongodb.casbah.Imports._
import net.scalytica.symbiotic.mongodb.docmanagement.Indexable
import net.scalytica.symbiotic.mongodb.{DefaultDB, WithMongoIndex}
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.PasswordAuthRepository
import repository.mongodb.bson.UserProfileBSONConverters.{
  LoginInfoBSONConverter,
  PasswordInfoBSONConverter
}

import scala.concurrent.Future

class MongoDBPasswordAuthRepository @Inject()(config: Configuration)
    extends PasswordAuthRepository
    with DefaultDB
    with LoginInfoBSONConverter
    with PasswordInfoBSONConverter
    with WithMongoIndex {

  override val configuration = config.underlying

  private[this] val logger = LoggerFactory.getLogger(this.getClass)

  override val collectionName = "authorization"

  private[this] val loginInfoKey    = "loginInfo"
  private[this] val passwordInfoKey = "passwordInfo"

  ensureIndex()

  override def ensureIndex(): Unit =
    index(
      List(Indexable(loginInfoKey)),
      collection
    )

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    Future.successful {
      collection
        .findOne(
          MongoDBObject(
            loginInfoKey -> loginInfoToBSON(loginInfo)
          )
        )
        .flatMap { dbo =>
          dbo.getAs[DBObject](passwordInfoKey).map(passwordInfoFromBSON)
        }
    }

  private[this] def upsert(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = Future.successful {
    try {
      collection.save[DBObject](
        MongoDBObject(
          loginInfoKey    -> loginInfoToBSON(loginInfo),
          passwordInfoKey -> passwordInfoToBSON(authInfo)
        )
      )
      authInfo
    } catch {
      case err: MongoException =>
        logger.error(
          s"There was an error saving the auth information " +
            s"for ${loginInfo.providerKey}"
        )
        throw err
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful {
    collection.remove(
      MongoDBObject(
        loginInfoKey -> loginInfoToBSON(loginInfo)
      )
    )
  }

  override def update(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = upsert(loginInfo, authInfo)

  override def save(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = upsert(loginInfo, authInfo)

  override def add(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = upsert(loginInfo, authInfo)
}
