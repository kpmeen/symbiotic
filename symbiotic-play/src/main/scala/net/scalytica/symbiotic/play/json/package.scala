package net.scalytica.symbiotic.play

import play.api.libs.json._

package object json {

  implicit def readsToIgnoreReads[T](r: JsPath): IgnoreJsPath = IgnoreJsPath(r)

}
