/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.DefaultDB
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, PersistentTypeConverters}
import models.customer.CustomerId
import models.project.ProjectId._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json._

/**
 * ...
 */
case class Project(
  _id: Option[ObjectId],
  v: Option[VersionStamp],
  id: Option[ProjectId],
  cid: CustomerId,
  title: String,
  description: Option[String],
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  // TODO: Add status field (active/stopped/done/...)
  hasLogo: Boolean = false) extends PersistentType

object Project extends PersistentTypeConverters with DateTimeConverters with DefaultDB with ObjectBSONConverters[Project] {

  override val collectionName: String = "project_memberships"

  implicit val projReads: Reads[Project] = Json.reads[Project]
  implicit val projWrites: Writes[Project] = Json.writes[Project]

  override def toBSON(p: Project): DBObject = {
    val builder = MongoDBObject.newBuilder
    p._id.foreach(builder += "_id" -> _)
    p.v.foreach(builder += "v" -> VersionStamp.toBSON(_))
    p.id.foreach(builder += "id" -> _.value)
    builder += "cid" -> p.cid.value
    builder += "title" -> p.title
    p.description.foreach(builder += "description" -> _)
    p.startDate.foreach(builder += "startDate" -> _.toDate)
    p.endDate.foreach(builder += "endDate" -> _.toDate)
    builder += "hasLogo" -> p.hasLogo

    builder.result()
  }

  override def fromBSON(d: DBObject): Project = {
    Project(
      _id = d.getAs[ObjectId]("_id"),
      v = d.getAs[DBObject]("v").map(VersionStamp.fromBSON),
      id = d.getAs[String]("id"),
      cid = d.as[String]("cid"),
      title = d.as[String]("title"),
      description = d.getAs[String]("title"),
      startDate = d.getAs[java.util.Date]("startDate"),
      endDate = d.getAs[java.util.Date]("endDate"),
      hasLogo = d.as[Boolean]("hasLogo")
    )
  }
}