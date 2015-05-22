/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import models.base.{PersistentTypeConverters, CompanyCode}
import org.bson.types.ObjectId
import play.api.libs.json._

/**
 * Representation of a Contractor organization in the system
 */
case class Contractor(
  _id: Option[ObjectId] = None,
  id: Option[OrganizationId] = None,
  code: CompanyCode,
  name: String,
  description: Option[String] = None,
  hasLogo: Option[Boolean] = None) extends Organization

object Contractor extends PersistentTypeConverters {

  implicit val contractorReads = Json.reads[Contractor]
  implicit val contractorWrites = Json.writes[Contractor]

}