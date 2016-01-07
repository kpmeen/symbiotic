/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.party

import com.google.inject.Singleton
import com.mongodb.casbah.Imports._
import core.lib.{Created, Failure, SuccessOrFailure, Updated}
import models.base.ShortName
import models.party.Organisation
import models.party.PartyBaseTypes.OrganisationId
import org.slf4j.LoggerFactory
import repository.OrganisationRepository
import repository.mongodb.bson.BSONConverters.Implicits._
import repository.mongodb.{DefaultDB, WithMongoIndex}

import scala.util.Try

@Singleton
class MongoDBOrganisationRepository extends OrganisationRepository with DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(this.getClass)

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
  def save(org: Organisation): SuccessOrFailure = {
    Try {
      val res = collection.save(org)
      logger.debug(res.toString)

      if (res.isUpdateOfExisting) Updated
      else Created

    }.recover {
      case t: Throwable =>
        logger.warn(s"An error occurred when saving $org", t)
        throw t
    }.getOrElse {
      Failure(s"Organisation $org could not be saved")
    }
  }

  /**
   *
   * @param oid
   * @return
   */
  def findById(oid: OrganisationId): Option[Organisation] =
    collection.findOne(MongoDBObject("id" -> oid.value)).map(oct => org_fromBSON(oct))

  /**
   *
   * @param sname
   * @return
   */
  def findByShortName(sname: ShortName): Option[Organisation] =
    collection.findOne(MongoDBObject("shortName" -> sname.code)).map(oct => org_fromBSON(oct))

}
