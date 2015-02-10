/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import models.core.{CompanyCode, CompanyId}
import play.api.libs.json._

/**
 * Representation of a customer (organization) in the system
 */
case class Company(
  id: Option[CompanyId],
  code: CompanyCode,
  name: String,
  description: Option[String] = None,
  hasLogo: Option[Boolean] = None) extends Organization


object Company {

  implicit val companyReads = Json.reads[Company]
  implicit val companyWrites = Json.writes[Company]

}