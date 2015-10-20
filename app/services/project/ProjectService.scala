/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.project

import com.mongodb.casbah.Imports._
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.party.PartyBaseTypes.OrganisationId
import models.project.{Project, ProjectId}
import org.slf4j.LoggerFactory

import scala.util.Try

object ProjectService extends DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(ProjectService.getClass)

  override val collectionName: String = "projects"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("oid", unique = false),
    Indexable("title", unique = false)
  ), collection)

  /**
   *
   * @param proj
   */
  def save(proj: Project): Unit =
    Try {
      val res = collection.save(proj)

      if (res.isUpdateOfExisting) logger.info("Updated existing project")
      else logger.info("Inserted new project")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"Project could not be saved", t)
    }

  /**
   *
   * @param pid
   * @return
   */
  def findById(pid: ProjectId): Option[Project] =
    collection.findOne(MongoDBObject("id" -> pid.value)).map(Project.fromBSON)

  /**
   *
   * @param oid
   * @return
   */
  def findByOrgId(oid: OrganisationId): Seq[Project] =
    collection.find(MongoDBObject("oid" -> oid.value)).map(Project.fromBSON).toSeq

}
