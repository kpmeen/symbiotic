/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.{WithBSONConverters, WithDateTimeConverters}
import core.mongodb.WithMongo
import models.customer.CustomerId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json._

case class Project(
  id: Option[ProjectId],
  cid: CustomerId,
  title: String,
  description: Option[String],
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  // TODO: Add status field (active/stopped/done/...)
  hasLogo: Boolean = false)


object Project extends WithDateTimeConverters with WithMongo with WithBSONConverters[Project] {

  override val collectionName: String = "project_memberships"

  implicit val projReads: Reads[Project] = Json.reads[Project]
  implicit val projWrites: Writes[Project] = Json.writes[Project]

  override implicit def toBSON(p: Project): DBObject = {
    val builder = MongoDBObject.newBuilder
    // TODO: Complete me
    p.id.foreach(builder += "_id" -> _.id)
    builder += "cid" -> p.cid.id
    builder += "title" -> p.title
    p.description.foreach(builder += "description" -> _)
    p.startDate.foreach(builder += "startDate" -> _.toDate)
    p.endDate.foreach(builder += "endDate" -> _.toDate)
    builder += "hasLogo" -> p.hasLogo

    builder.result()
  }

  override implicit def fromBSON(d: DBObject): Project = {
    Project(
      id = d.getAs[ObjectId]("_id"),
      cid = d.as[ObjectId]("cid"),
      title = d.as[String]("title"),
      description = d.getAs[String]("title"),
      startDate = d.getAs[java.util.Date]("startDate"),
      endDate = d.getAs[java.util.Date]("endDate"),
      hasLogo = d.as[Boolean]("hasLogo")
    )
  }
}