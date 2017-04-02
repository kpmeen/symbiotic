package net.scalytica.symbiotic.play.json

import net.scalytica.symbiotic.data.{Path, PathNode}
import play.api.libs.json._

object Implicits extends PathFormat {}

trait PathFormat {
  private val reads  = __.readNullable[String].map(Path.fromDisplay)
  private val writes = Writes(p => JsString(Path.toDisplay(p)))

  implicit val pathFormat: Format[Path]         = Format(reads, writes)
  implicit val pathNodeFormat: Format[PathNode] = Json.format[PathNode]
}
