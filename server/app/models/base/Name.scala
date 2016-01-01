/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import play.api.libs.json.Json

/**
 * Represents the full name of an individual (a.k.a. person).
 */
case class Name(first: Option[String], middle: Option[String], last: Option[String])

object Name {
  implicit val nameFormat = Json.format[Name]
}
