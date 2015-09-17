/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.casbah.Imports._
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, PersistentTypeConverters}
import models.party.PartyBaseTypes.OrgId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try

/**
 * TODO: Comment me
 */
case class Project(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  id: Option[ProjectId] = None,
  oid: OrgId,
  title: String,
  description: Option[String] = None,
  startDate: Option[DateTime] = None,
  endDate: Option[DateTime] = None,
  // TODO: Add status field (active/stopped/done/...)
  hasLogo: Boolean = false) extends PersistentType

object Project extends PersistentTypeConverters with DateTimeConverters with DefaultDB with WithMongoIndex with ObjectBSONConverters[Project] {

  val logger = LoggerFactory.getLogger(classOf[Project])

  implicit val projReads: Reads[Project] = Json.reads[Project]
  implicit val projWrites: Writes[Project] = Json.writes[Project]

  override val collectionName: String = "projects"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("oid", unique = false),
    Indexable("title", unique = false)
  ), collection)

  implicit override def toBSON(p: Project): DBObject = {
    val builder = MongoDBObject.newBuilder
    p._id.foreach(builder += "_id" -> _)
    p.v.foreach(builder += "v" -> VersionStamp.toBSON(_))
    p.id.foreach(builder += "id" -> _.value)
    builder += "oid" -> p.oid.value
    builder += "title" -> p.title
    p.description.foreach(builder += "description" -> _)
    p.startDate.foreach(builder += "startDate" -> _.toDate)
    p.endDate.foreach(builder += "endDate" -> _.toDate)
    builder += "hasLogo" -> p.hasLogo

    builder.result()
  }

  implicit override def fromBSON(d: DBObject): Project = {
    Project(
      _id = d.getAs[ObjectId]("_id"),
      v = d.getAs[DBObject]("v").map(VersionStamp.fromBSON),
      id = d.getAs[String]("id"),
      oid = d.as[String]("oid"),
      title = d.as[String]("title"),
      description = d.getAs[String]("title"),
      startDate = d.getAs[java.util.Date]("startDate"),
      endDate = d.getAs[java.util.Date]("endDate"),
      hasLogo = d.getAs[Boolean]("hasLogo").getOrElse(false)
    )
  }

  /**
   *
   * @param proj
   */
  def save(proj: Project): Unit = {
    Try {
      val res = collection.save(proj)

      if (res.isUpdateOfExisting) logger.info("Updated existing project")
      else logger.info("Inserted new project")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"Project could not be saved", t)
    }
  }

  /**
   *
   * @param pid
   * @return
   */
  def findById(pid: ProjectId): Option[Project] =
    collection.findOne(MongoDBObject("id" -> pid.value)).map(oct => fromBSON(oct))

  /**
   *
   * @param oid
   * @return
   */
  def findByOrgId(oid: OrgId): Seq[Project] =
    collection.find(MongoDBObject("oid" -> oid.value)).map(oct => fromBSON(oct)).toSeq
}