/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * A (max) 8 character long alphanumerical code as a short-name to represent a company.
 */
case class ShortName(code: String)

object ShortName {

  implicit val companyCodeReads = __.read[String](maxLength[String](8)).map(ShortName(_))
  implicit val companyCodeWrites = Writes {
    (cc: ShortName) => JsString(cc.code)
  }

}