package net.scalytica.symbiotic.play.json

import java.util.UUID

import net.scalytica.symbiotic.data.PersistentType.{UserStamp, VersionStamp}
import play.api.libs.json._

trait PersistentTypeFormatters {

  implicit val UsrStampFmt: Format[UserStamp]    = Json.format[UserStamp]
  implicit val VerStampFmt: Format[VersionStamp] = Json.format[VersionStamp]
  implicit val uuidFormat: Format[UUID] = Format(
    fjs = __.read[String].map(s => UUID.fromString(s)),
    tjs = Writes(a => JsString(a.toString))
  )

}
