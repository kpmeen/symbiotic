package net.scalytica.symbiotic.models

import org.scalajs.dom

case class User(name: String, pass: String)

object User {
  val sessionKey = "PLAY_SESSION"

  /**
   * @return If no sessionKey cookie is found, returns false
   */
  def isLoggedIn() : Boolean = {
    val rawCookie = dom.document.cookie
    if (rawCookie.isEmpty) {
      return false
    }

    val keyValuePairs = rawCookie.split(';')
        .filter(_.startsWith(sessionKey))(0)
        .substring(rawCookie.indexOf('-') + 1)
        .split("""&""")
        .map(_.split("""=""") match {
          case Array(k, v) => (k, v)
        })

    keyValuePairs.length == keyValuePairs.count(t => t._1.nonEmpty && t._2.nonEmpty)
  }
}
