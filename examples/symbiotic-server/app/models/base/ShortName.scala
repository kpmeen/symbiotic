package models.base

import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * A (max) 8 character long alphanumerical code as a short-name to represent
 * a company.
 */
case class ShortName(code: String)

object ShortName {

  private val MaxLength = 8

  implicit val companyCodeReads: Reads[ShortName] =
    JsPath.read[String](maxLength[String](MaxLength)).map(ShortName.apply)

  implicit val companyCodeWrites: Writes[ShortName] = Writes { n =>
    JsString(n.code)
  }

}
