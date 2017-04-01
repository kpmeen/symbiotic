/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package models.base

import play.api.libs.json.Json

/**
 * Represents the full name of an individual (a.k.a. person).
 */
case class Name(
    first: Option[String] = None,
    middle: Option[String] = None,
    last: Option[String] = None
)

object Name {
  implicit val nameFormat = Json.format[Name]
}
