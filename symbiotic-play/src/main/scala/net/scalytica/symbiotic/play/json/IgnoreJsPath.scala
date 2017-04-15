package net.scalytica.symbiotic.play.json

import play.api.libs.json._

case class IgnoreJsPath(jsp: JsPath) {

  def readIgnore[T](implicit r: Reads[T]): Reads[Option[T]] =
    Reads(_ => JsSuccess(None))

  def writeIgnore[T](implicit w: Writes[T]): OWrites[Option[T]] =
    OWrites.apply[Option[T]](_ => Json.obj())

}
