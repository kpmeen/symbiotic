package models.base

import play.api.libs.json.Reads._
import play.api.libs.json._

sealed abstract class Gender(val value: Char) {
  val strValue = value.toString
}

case object Male   extends Gender('m')
case object Female extends Gender('f')

object Gender {
  implicit val genderReads: Reads[Gender] = __
    .read[String]
    .filter(
      JsonValidationError("Gender can only be m(ale) or f(emale)")
    )(c => c == Male.strValue || c == Female.strValue)
    .map {
      case Male.strValue   => Male
      case Female.strValue => Female
    }

  implicit val genderWrites: Writes[Gender] = Writes { (g: Gender) =>
    JsString(g.strValue)
  }

  def fromString(s: String): Option[Gender] = {
    s match {
      case Male.strValue   => Some(Male)
      case Female.strValue => Some(Female)
      case _               => None
    }
  }

  def fromChar(c: Char): Option[Gender] = {
    c match {
      case Male.value   => Some(Male)
      case Female.value => Some(Female)
      case _            => None
    }
  }

  def unsafeFromString(s: String): Gender =
    fromString(s).getOrElse {
      throw new IllegalArgumentException("Gender must be either 'f' or 'm'.")
    }

  def values = Seq(Female, Male)

}
