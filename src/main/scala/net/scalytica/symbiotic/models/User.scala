package net.scalytica.symbiotic.models

import org.scalajs.dom

case class User(name: String, pass: String)

object User {
  val sessionKey = "PLAY_SESSION"

  /**
   * @return If no sessionKey cookie is found, returns false
   */
  def isLoggedIn: Boolean = Option(dom.document.cookie).fold(false) { rawCookie =>
    val kvp = rawCookie.split(';').find(_.startsWith(sessionKey)).map { mc =>
      mc.substring(rawCookie.indexOf('-') + 1).split( """&""").map(_.split( """=""") match {
        case Array(k, v) => (k, v)
      })
    }.getOrElse(return false)

    kvp.length == kvp.count(t => t._1.nonEmpty && t._2.nonEmpty)
  }
}
