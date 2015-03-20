/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.customer

import core.converters.WithIdConverters
import models.base.Id
import models.parties.OrganizationId
import org.bson.types.ObjectId

/**
 * Representation of a "paying" Customer of the system.
 *
 * TODO: Add relevant information about a Customer...
 * - Payment method
 * - registration date
 * - status (active/disabled [with reason])
 * - Billing information
 * - and so on...
 */
case class Customer(
  id: CustomerId,
  orgId: OrganizationId)

/**
 * Id implementation for CustomerId.
 */
case class CustomerId(id: ObjectId) extends Id

object CustomerId extends WithIdConverters[CustomerId] {
  implicit val companyIdReads = reads(CustomerId.apply)
  implicit val companyIdWrites = writes

  override implicit def asId(oid: ObjectId): CustomerId = CustomerId(oid)

  override implicit def asId(s: String): CustomerId = CustomerId(new ObjectId(s))
}
