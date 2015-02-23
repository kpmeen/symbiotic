/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.parties

import models.core.{Id, WithIdTransformers}
import org.bson.types.ObjectId
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Id implementation for CompanyId.
 */
case class ContractorId(id: ObjectId) extends Id

object ContractorId extends WithIdTransformers {
  implicit val companyIdReads = reads[ContractorId](ContractorId.apply)
  implicit val companyIdWrites = writes[ContractorId]
}

/**
 * A (max) 8 character long alphanumerical code as a short-name to represent a company.
 */
case class CompanyCode(code: String)

object CompanyCode {

  implicit val companyCodeReads = __.read[String](maxLength[String](8)).map(CompanyCode(_))
  implicit val companyCodeWrites = Writes {
    (cc: CompanyCode) => JsString(cc.code)
  }

}