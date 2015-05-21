/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.customer

import core.converters.WithDBIdConverters
import models.base.DBId
import models.parties.OrganizationId

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
case class CustomerId(value: String) extends DBId

object CustomerId extends WithDBIdConverters[CustomerId] {
  implicit val companyIdReads = reads(CustomerId.apply)
  implicit val companyIdWrites = writes

  override implicit def asId(s: String): CustomerId = CustomerId(s)
}
