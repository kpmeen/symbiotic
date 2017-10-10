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

  def emptyAsOption(
      first: Option[String],
      middle: Option[String],
      last: Option[String]
  ): Option[Name] = {
    if (first.isEmpty && middle.isEmpty && last.isEmpty) None
    else Some(Name(first, middle, last))
  }
}
