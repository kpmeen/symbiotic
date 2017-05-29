package repository.mongodb.party

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.Imports._
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.mongodb.{DefaultGridFS, WithMongoIndex}
import org.slf4j.LoggerFactory
import play.api.Configuration
import repository.mongodb.AvatarRepository
import repository.mongodb.bson.UserProfileBSONConverters.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Provides services to interact with avatars associated with user profiles
 */
@Singleton
class MongoDBAvatarRepository @Inject()(
    config: Configuration
) extends AvatarRepository
    with DefaultGridFS
    with WithMongoIndex {

  override def configuration = config.underlying

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def ensureIndex(): Unit =
    index(List(Indexable("filename", unique = true)), collection)

  override def save(
      a: Avatar
  )(implicit ec: ExecutionContext): Future[Option[UUID]] = Future {
    val id = UUID.randomUUID()
    val old = collection
      .find(
        MongoDBObject("filename" -> a.metadata.uid.value)
      )
      .map(dbo => UUID.fromString(dbo.as[String]("_id")))
      .toSeq

    Try {
      a.stream
        .flatMap(
          s =>
            gfs(s) { gf =>
              gf.filename = a.filename
              a.contentType.foreach(gf.contentType = _)
              gf.metaData = avatarmd_toBSON(a.metadata)
              gf += ("_id" -> id.toString) // TODO: Verify this with the tests...
          }
        )
        .map { oid =>
          remove(a.metadata.uid, old)
          id
        }
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $a", e)
        None
    }.toOption.flatten
  }

  override def remove(
      uid: UserId,
      ids: Seq[UUID]
  )(implicit ec: ExecutionContext): Future[Unit] = Future {
    if (0 < ids.size) {
      val oids = MongoDBList.newBuilder
      ids.foreach(oids += _.toString)
      val b = MongoDBObject(
        // Casbah...inconsistent piece of fucking shit!
        "_id"      -> MongoDBObject("$in" -> oids.result()),
        "filename" -> uid.value
      )
      gfs.remove(b)
      logger.debug(s"Removed ${ids.size} avatar images for $uid.")
    } else {
      logger.debug(s"No avatar images to remove.")
    }
  }

  override def remove(
      uid: UserId
  )(implicit ec: ExecutionContext): Future[Unit] = Future {
    gfs.remove(MongoDBObject("filename" -> uid.value))
  }

  override def get(
      uid: UserId
  )(implicit ec: ExecutionContext): Future[Option[Avatar]] = Future {
    gfs.findOne(MongoDBObject("filename" -> uid.value))
  }

}
