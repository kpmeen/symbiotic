package models.base

import play.api.libs.json.{Format, Json}

/**
 * Represents the full name of an individual (a.k.a. person).
 */
case class Name(
    first: Option[String] = None,
    middle: Option[String] = None,
    last: Option[String] = None
)

object Name {
  implicit val nameFormat: Format[Name] = Json.format[Name]
}
