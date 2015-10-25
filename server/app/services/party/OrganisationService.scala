/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.mongodb.casbah.Imports._
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.ShortName
import models.party.Organisation
import models.party.PartyBaseTypes.OrganisationId
import org.slf4j.LoggerFactory

import scala.util.Try

object OrganisationService extends DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(OrganisationService.getClass)

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("shortName", unique = true),
    Indexable("name", unique = false)
  ), collection)

  override val collectionName: String = "organisations"

  ensureIndex()

  /**
   *
   * @param org
   */
  def save(org: Organisation): Unit = {
    Try {
      val res = collection.save(org)

      if (res.isUpdateOfExisting) logger.info("Updated existing organisation")
      else logger.info("Inserted new organisation")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"Organisation could not be saved", t)
    }
  }

  /**
   *
   * @param oid
   * @return
   */
  def findById(oid: OrganisationId): Option[Organisation] =
    collection.findOne(MongoDBObject("id" -> oid.value)).map(oct => Organisation.fromBSON(oct))

  /**
   *
   * @param sname
   * @return
   */
  def findByShortName(sname: ShortName): Option[Organisation] =
    collection.findOne(MongoDBObject("shortName" -> sname.code)).map(oct => Organisation.fromBSON(oct))

}
