package models.base

import play.api.libs.json.Reads._
import play.api.libs.json._

sealed trait Gender {
  val value: Char
}

object Gender {
  implicit val genderReads: Reads[Gender] = __
    .read[String]
    .filter(
      JsonValidationError("Gender can only be m(ale) or f(emale)")
    )(c => c == "m" || c == "f")
    .map {
      case "m" => Male()
      case "f" => Female()
    }

  implicit val genderWrites: Writes[Gender] = Writes { (g: Gender) =>
    JsString(g.value.toString)
  }

  def fromString(s: String): Option[Gender] = {
    s match {
      case "m" => Some(Male())
      case "f" => Some(Female())
      case _   => None
    }
  }

  case class Male() extends Gender {
    val value = 'm'
  }

  case class Female() extends Gender {
    val value = 'f'
  }

}
