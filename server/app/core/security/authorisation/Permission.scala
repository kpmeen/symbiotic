/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.security.authorisation

import play.api.libs.json._

sealed trait Permission

object Permission {

  val read = "r"
  val create = "c"
  val update = "u"

  def fromString(c: String): Option[Permission] = {
    c match {
      case `read` => Some(Read)
      case `create` => Some(Create)
      case `update` => Some(Update)
      case _ => None
    }
  }

  def asString(p: Permission): String = {
    p match {
      case Read => read
      case Create => create
      case Update => update
    }
  }

  implicit val reads: Reads[Permission] = Reads(jsv =>
    fromString(jsv.as[String]).map(p => JsSuccess(p)).getOrElse(JsError(error = "Unknown permission value")))

  implicit val writes: Writes[Permission] = Writes(p => JsString(asString(p)))

}

case object Read extends Permission

case object Create extends Permission

case object Update extends Permission

