package net.scalytica.symbiotic.models

import net.scalytica.symbiotic.logger._
import org.scalajs.dom

case class User(name: String, pass: String)

object User {
  val sessionKey = "SYMBIOTIC_USER"

  /**
   * @return If no sessionKey cookie is found, returns false
   */
  def isLoggedIn: Boolean = {
    log.info(s"Raw cookie is: ${dom.document.cookie}")
    Option(dom.document.cookie).fold(false) { rawCookie =>
      val kvp = rawCookie.split(';').find(_.startsWith(sessionKey)).map { mc =>
        mc.split("=").tail match {
          case Array(k, v) =>
            log.info(s"Got key $k with value: $v")
            (k, v)
        }
      }
      kvp.size == kvp.count(t => t._1.nonEmpty && t._2.nonEmpty)
    }
  }
}
