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

  private val MaxLength = 8

  implicit val companyCodeReads = __
    .read[String](maxLength[String](MaxLength))
    .map(ShortName(_)) // scalastyle:ignore
  implicit val companyCodeWrites = Writes { (cc: ShortName) =>
    JsString(cc.code)
  }

}
