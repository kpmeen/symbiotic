package repository.mongodb.silhouette

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mongodb.MongoException
import com.mongodb.casbah.Imports._
import net.scalytica.symbiotic.mongodb.docmanagement.Indexable
import net.scalytica.symbiotic.mongodb.{DefaultDB, WithMongoIndex}
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.OAuth2Repository
import repository.mongodb.bson.UserProfileBSONConverters.{
  LoginInfoBSONConverter,
  OAuth2InfoBSONConverter
}

import scala.concurrent.Future

class MongoDBOAuth2Repository @Inject()(config: Configuration)
    extends OAuth2Repository
    with DefaultDB
    with LoginInfoBSONConverter
    with OAuth2InfoBSONConverter
    with WithMongoIndex {

  private[this] val logger = LoggerFactory.getLogger(this.getClass)

  override val configuration  = config.underlying
  override val collectionName = "oauth"

  private[this] val LoginInfoKey  = "loginInfo"
  private[this] val OAuth2InfoKey = "oauth2Info"

  ensureIndex()

  override def ensureIndex(): Unit =
    logger.warn(s"No index for ${this.getClass.getCanonicalName}")

  index(List(Indexable(LoginInfoKey)), collection)

  private[this] def upsert(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = Future.successful {
    try {
      val maybeRes = collection.findOne(
        MongoDBObject(
          LoginInfoKey -> loginInfoToBSON(loginInfo)
        )
      )

      val builder = MongoDBObject.newBuilder
      maybeRes.foreach(dbo => builder += "_id" -> dbo.as[ObjectId]("_id"))
      builder += LoginInfoKey  -> loginInfoToBSON(loginInfo)
      builder += OAuth2InfoKey -> oauth2InfoToBSON(authInfo)

      collection.save[DBObject](builder.result())
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

  override def update(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = upsert(loginInfo, authInfo)

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful {
    collection.remove(
      MongoDBObject(
        LoginInfoKey -> loginInfoToBSON(loginInfo)
      )
    )
  }

  override def save(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = upsert(loginInfo, authInfo)

  override def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] =
    Future.successful {
      collection
        .findOne(
          MongoDBObject(
            LoginInfoKey -> loginInfoToBSON(loginInfo)
          )
        )
        .flatMap { dbo =>
          dbo.getAs[DBObject](OAuth2InfoKey).map(oauth2InfoFromBSON)
        }
    }

  override def add(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = upsert(loginInfo, authInfo)
}
