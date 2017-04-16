package net.scalytica.symbiotic.play.json

import play.api.libs.json._

case class IgnoreJsPath(jsp: JsPath) {

  def readIgnore[T]: Reads[Option[T]] = Reads(_ => JsSuccess(None))

  def writeIgnore[T]: OWrites[Option[T]] = OWrites.apply(_ => Json.obj())

}
