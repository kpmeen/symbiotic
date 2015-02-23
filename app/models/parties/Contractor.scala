/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import play.api.libs.json._

/**
 * Representation of a customer (organization) in the system
 */
case class Contractor(
  id: Option[ContractorId],
  code: CompanyCode,
  name: String,
  description: Option[String] = None,
  hasLogo: Option[Boolean] = None) extends Organization


object Contractor {

  implicit val contractorReads = Json.reads[Contractor]
  implicit val contractorWrites = Json.writes[Contractor]

}