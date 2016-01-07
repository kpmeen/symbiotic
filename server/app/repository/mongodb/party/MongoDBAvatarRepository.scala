/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.party

import com.google.inject.Singleton
import com.mongodb.casbah.Imports._
import models.party.Avatar
import models.party.PartyBaseTypes.UserId
import org.slf4j.LoggerFactory
import repository.AvatarRepository
import repository.mongodb.bson.BSONConverters.Implicits._
import repository.mongodb.{DefaultGridFS, WithMongoIndex}

import scala.util.Try

/**
 * Provides services to interact with avatars associated with user profiles
 */
@Singleton
class MongoDBAvatarRepository extends AvatarRepository[ObjectId] with DefaultGridFS with WithMongoIndex {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def ensureIndex(): Unit = index(List(Indexable("filename", unique = true)), collection)

  override def save(a: Avatar): Option[ObjectId] = {
    val old = collection.find(MongoDBObject("filename" -> a.metadata.uid.value)).map(_.as[ObjectId]("_id")).toSeq

    Try {
      a.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = a.filename
        a.contentType.foreach(gf.contentType = _)
        gf.metaData = avatarmd_toBSON(a.metadata)
      }).map { oid =>
        remove(a.metadata.uid, old)
        oid.asInstanceOf[ObjectId]
      }
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $a", e)
        None
    }.toOption.flatten
  }

  override def remove(uid: UserId, ids: Seq[ObjectId]): Unit = {
    if (0 < ids.size) {
      val oids = MongoDBList.newBuilder
      ids.foreach(oids += _)
      val b = MongoDBObject(
        // Casbah...inconsistent piece of fucking shit!
        "_id" -> MongoDBObject("$in" -> oids.result()),
        "filename" -> uid.value
      )
      gfs.remove(b)
      logger.debug(s"Removed ${ids.size} avatar images for $uid.")
    } else {
      logger.debug(s"No avatar images to remove.")
    }
  }

  override def remove(uid: UserId): Unit = gfs.remove(MongoDBObject("filename" -> uid.value))

  override def get(uid: UserId): Option[Avatar] = gfs.findOne(MongoDBObject("filename" -> uid.value))

}
