/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.customer

import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, PersistentTypeConverters}
import models.parties.CompanyId
import org.bson.types.ObjectId
import play.api.libs.json._

/**
 * Representation of a "PAYING" Customer of the system.
 *
 * TODO: Add relevant information about a Customer...
 * - Payment method
 * - registration date
 * - status (active/disabled [with reason])
 * - Billing information
 * - and so on...
 *
 *
 * @param _id the internal MongoDB ObjectId
 * @param v an optional version stamp
 * @param cid the unique customer ID.
 * @param orgId Reference to the ID of the full company data representation for this customer.
 */
case class Customer(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  cid: CustomerId,
  orgId: CompanyId) extends PersistentType

object Customer extends PersistentTypeConverters {

  implicit val r: Format[Customer] = Json.format[Customer]

}