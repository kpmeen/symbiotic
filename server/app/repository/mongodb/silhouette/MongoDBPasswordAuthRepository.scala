/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.silhouette

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mongodb.MongoException
import com.mongodb.casbah.Imports._
import net.scalytica.symbiotic.mongodb.{DefaultDB, WithMongoIndex}
import net.scalytica.symbiotic.persistence.PasswordAuthRepository
import org.slf4j.LoggerFactory
import play.api.Configuration
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.{
  LoginInfoBSONConverter,
  PasswordInfoBSONConverter
}
import repository.mongodb.WithMongoIndex

import scala.concurrent.Future
import scala.util.Try

class MongoDBPasswordAuthRepository @Inject()(val configuration: Configuration)
    extends PasswordAuthRepository
    with DefaultDB
    with LoginInfoBSONConverter
    with PasswordInfoBSONConverter
    with WithMongoIndex {

  val logger = LoggerFactory.getLogger(this.getClass)

  override val collectionName = "authorization"
  private val LoginInfoKey    = "loginInfo"

  private val PasswordInfoKey = "passwordInfo"

  ensureIndex()

  override def ensureIndex(): Unit =
    index(
      List(
        Indexable(LoginInfoKey)
      ),
      collection
    )

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    Future.successful {
      collection
        .findOne(
          MongoDBObject(
            LoginInfoKey -> loginInfo_toBSON(loginInfo)
          )
        )
        .flatMap { dbo =>
          dbo.getAs[DBObject](PasswordInfoKey).map(passwordInfo_fromBSON)
        }
    }

  private def upsert(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = Future.successful {
    Try {
      collection.save[DBObject](
        MongoDBObject(
          LoginInfoKey    -> loginInfo_toBSON(loginInfo),
          PasswordInfoKey -> passwordInfo_toBSON(authInfo)
        )
      )
      authInfo
    }.recover {
      case err: MongoException =>
        logger.error(
          s"There was an error saving the auth information " +
            s"for ${loginInfo.providerKey}"
        )
        throw err
    }.get
  }

  override def update(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = upsert(loginInfo, authInfo)

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful {
    collection.remove(
      MongoDBObject(
        LoginInfoKey -> loginInfo_toBSON(loginInfo)
      )
    )
  }

  override def save(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] =
    upsert(loginInfo, authInfo)

  override def add(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] =
    upsert(loginInfo, authInfo)
}
