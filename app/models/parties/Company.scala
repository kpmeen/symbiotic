/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import models.base.PersistentType.VersionStamp
import models.base.{ShortName, PersistentTypeConverters}
import org.bson.types.ObjectId
import play.api.libs.json._

/**
 * Representation of a Company/Organization in the system
 */
case class Company(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  id: Option[CompanyId] = None,
  shortName: ShortName,
  name: String,
  description: Option[String] = None,
  hasLogo: Option[Boolean] = None) extends Organization

object Company extends PersistentTypeConverters {

  implicit val contractorReads = Json.reads[Company]
  implicit val contractorWrites = Json.writes[Company]

}