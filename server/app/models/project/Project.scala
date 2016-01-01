/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.DateTimeConverters
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, PersistentTypeConverters}
import models.party.PartyBaseTypes.OrganisationId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._

/**
 * TODO: Comment me
 */
case class Project(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  id: Option[ProjectId] = None,
  oid: OrganisationId,
  title: String,
  description: Option[String] = None,
  startDate: Option[DateTime] = None,
  endDate: Option[DateTime] = None,
  // TODO: Add status field (active/stopped/done/...)
  hasLogo: Boolean = false
) extends PersistentType

object Project extends PersistentTypeConverters with DateTimeConverters {

  val logger = LoggerFactory.getLogger(classOf[Project])

  implicit val projReads: Reads[Project] = Json.reads[Project]
  implicit val projWrites: Writes[Project] = Json.writes[Project]

}