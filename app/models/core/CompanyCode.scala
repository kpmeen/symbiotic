/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import play.api.libs.json.Reads._
import play.api.libs.json._

case class CompanyCode(code: String)

object CompanyCode {

  implicit val companyCodeReads = __.read[String](maxLength[String](4)).map(CompanyCode(_))
  implicit val companyCodeWrites = Writes {
    (cc: CompanyCode) => JsString(cc.code)
  }

}