package net.scalytica.symbiotic.models.dman

import play.api.libs.json.{Format, Json}

case class Owner(ownerId: String, ownerType: String)

object Owner {

  implicit val format: Format[Owner] = Json.format[Owner]

}
