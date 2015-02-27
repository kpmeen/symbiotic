/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.customer

import models.core.{WithIdTransformers, Id}
import org.bson.types.ObjectId

case class Customer(id: CustomerId)

/**
 * Id implementation for CustomerId.
 */
case class CustomerId(id: ObjectId) extends Id

object CustomerId extends WithIdTransformers {
  implicit val companyIdReads = reads[CustomerId](CustomerId.apply)
  implicit val companyIdWrites = writes[CustomerId]
}
