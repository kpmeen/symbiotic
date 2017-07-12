package net.scalytica.symbiotic.json

import play.api.libs.json._

case class IgnoreJsPath(jsp: JsPath) {

  def readIgnore[T]: Reads[Option[T]] = Reads(_ => JsSuccess(None))

  def writeIgnore[T]: OWrites[Option[T]] = OWrites.apply(_ => Json.obj())

  def formatIgnore[T]: OFormat[Option[T]] =
    OFormat(readIgnore[T], writeIgnore[T])

}
