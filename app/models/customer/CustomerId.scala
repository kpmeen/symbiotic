/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.customer

import converters.IdConverters
import models.base.Id

/**
 * Id implementation for CustomerId.
 */
case class CustomerId(value: String) extends Id

object CustomerId extends IdConverters[CustomerId] {

  implicit val companyIdReads = reads(CustomerId.apply)
  implicit val companyIdWrites = writes

  override implicit def asId(s: String): CustomerId = CustomerId(s)
}
