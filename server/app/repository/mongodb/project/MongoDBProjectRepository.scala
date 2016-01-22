/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.project

import com.google.inject.Singleton
import com.mongodb.casbah.Imports._
import core.lib.{Created, Failure, SuccessOrFailure, Updated}
import models.party.PartyBaseTypes.OrganisationId
import models.project.{Project, ProjectId}
import org.slf4j.LoggerFactory
import repository.ProjectRepository
import repository.mongodb.bson.BSONConverters.Implicits._
import repository.mongodb.{DefaultDB, WithMongoIndex}

import scala.util.Try

@Singleton
class MongoDBProjectRepository extends ProjectRepository with DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(this.getClass)

  override val collectionName: String = "projects"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("oid", unique = false),
    Indexable("title", unique = false)
  ), collection)

  /**
   *
   * @param proj
   */
  def save(proj: Project): SuccessOrFailure =
    Try {
      val res = collection.save(proj)
      logger.debug(res.toString)

      if (res.isUpdateOfExisting) Updated
      else Created
    }.recover {
      case t =>
        logger.warn(s"An error occurred when saving $proj", t)
        throw t
    }.getOrElse {
      Failure(s"Project $proj could not be saved")
    }

  /**
   *
   * @param pid
   * @return
   */
  def findById(pid: ProjectId): Option[Project] = {
    logger.info(s"Building query to find Project with $pid")
    collection.findOne(MongoDBObject("_id" -> pid.value)).map(proj_fromBSON)
  }

  /**
   *
   * @param oid
   * @return
   */
  def listByOrgId(oid: OrganisationId): Seq[Project] =
    collection.find(MongoDBObject("oid" -> oid.value)).map(proj_fromBSON).toSeq

}
