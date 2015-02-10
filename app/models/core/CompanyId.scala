/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import org.bson.types.ObjectId

/**
 * Id implementation for CompanyId.
 */
case class CompanyId(id: ObjectId) extends Id

object CompanyId extends WithIdTransformers {
  implicit val companyIdReads = reads[CompanyId](CompanyId.apply)
  implicit val companyIdWrites = writes[CompanyId]
}