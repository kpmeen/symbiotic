/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mongodb.MongoException
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory
import repository.OAuth2Repository
import repository.mongodb.{WithMongoIndex, DefaultDB}
import repository.mongodb.bson.BSONConverters.{OAuth2InfoBSONConverter, LoginInfoBSONConverter}

import scala.concurrent.Future
import scala.util.Try

class MongoDBOAuth2Repository
    extends OAuth2Repository
    with DefaultDB
    with LoginInfoBSONConverter
    with OAuth2InfoBSONConverter
    with WithMongoIndex {

  val logger = LoggerFactory.getLogger(this.getClass)

  override val collectionName = "oauth"
  private val LoginInfoKey = "loginInfo"

  private val OAuth2InfoKey = "oauth2Info"

  ensureIndex()

  override def ensureIndex(): Unit = logger.warn(s"No index for ${this.getClass.getCanonicalName}")
  //    index(List(
  //      Indexable(LoginInfoKey, unique = false)
  //    ), collection)

  private def upsert(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = Future.successful {
    Try {
      collection.save[DBObject](
        MongoDBObject(
          LoginInfoKey -> loginInfo_toBSON(loginInfo),
          OAuth2InfoKey -> oauth2Info_toBSON(authInfo)
        )
      )
      authInfo
    }.recover {
      case err: MongoException =>
        logger.error(s"There was an error saving the auth information for ${loginInfo.providerKey}")
        throw err
    }.get
  }

  override def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] =
    upsert(loginInfo, authInfo)

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful {
    collection.remove(MongoDBObject(
      LoginInfoKey -> loginInfo_toBSON(loginInfo)
    ))
  }

  override def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] =
    upsert(loginInfo, authInfo)

  override def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = Future.successful {
    collection.findOne(MongoDBObject(
      LoginInfoKey -> loginInfo_toBSON(loginInfo)
    )).flatMap { dbo =>
      dbo.getAs[DBObject](OAuth2InfoKey).map(oauth2Info_fromBSON)
    }
  }

  override def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] =
    upsert(loginInfo, authInfo)
}
