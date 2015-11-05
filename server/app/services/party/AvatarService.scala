/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.mongodb.casbah.Imports._
import core.mongodb.{DefaultGridFS, WithMongoIndex}
import models.party.Avatar
import models.party.PartyBaseTypes.UserId
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * Provides services to interact with avatars associated with user profiles
 */
object AvatarService extends DefaultGridFS with WithMongoIndex {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def ensureIndex(): Unit = index(List(Indexable("filename", unique = true)), collection)

  /**
   * Saves a new Avatar for the User specified in the metadata.
   * Only 1 avatar image per user will be kept, so this method will ensure
   * that old avatar images are cleaned up after adding the new one.
   *
   * @param a the Avatar to save
   * @return an Option that will contain the ObjectId of the added avatar if successful
   */
  def save(a: Avatar): Option[ObjectId] = {
    val old = collection.find(MongoDBObject("filename" -> a.metadata.uid.value)).map(_.as[ObjectId]("_id")).toSeq

    Try {
      a.stream.flatMap(s => gfs(s) { gf =>
        gf.filename = a.filename
        a.contentType.foreach(gf.contentType = _)
        gf.metaData = a.metadata
      }).map { oid =>
        remove(a.metadata.uid, old)
        oid.asInstanceOf[ObjectId]
      }
    }.recover {
      case e: Throwable =>
        logger.error(s"An error occurred trying to save $a", e)
        None
    }.get
  }

  /**
   *
   * @param uid UserId to remove files for.
   * @param ids a collection of the ObjectId of files to remove
   */
  def remove(uid: UserId, ids: Seq[ObjectId]): Unit = {
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

  /**
   * Removes _all_ avatar images where filename equals the uid
   *
   * @param uid UserId to remove avatar images for
   */
  def remove(uid: UserId): Unit = gfs.remove(MongoDBObject("filename" -> uid.value))

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param uid UserId
   * @return Option[File]
   */
  def get(uid: UserId): Option[Avatar] = gfs.findOne(MongoDBObject("filename" -> uid.value))

}
