package net.scalytica.symbiotic.models.dman

import play.api.libs.json.{Format, Json}

case class Lock(by: String, date: String)

object Lock {

  implicit val format: Format[Lock] = Json.format[Lock]

}
