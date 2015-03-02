/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.DBObject
import core.{WithMongo, WithBSONConverters}
import models.base.mapping.WithDateTimeMapping
import models.customer.CustomerId
import org.joda.time.DateTime
import play.api.libs.json._

case class Project(
  id: ProjectId,
  cid: CustomerId,
  title: String,
  description: Option[String],
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  // TODO: Add status field (active/stopped/done/...)
  hasLogo: Boolean = false)


object Project extends WithDateTimeMapping with WithMongo with WithBSONConverters[Project] {

  override val collectionName: String = "project_memberships"

  implicit val projReads: Reads[Project] = Json.reads[Project]
  implicit val projWrites: Writes[Project] = Json.writes[Project]

  override implicit def toBSON(x: Project): DBObject = ???

  override implicit def fromBSON(dbo: DBObject): Project = ???
}