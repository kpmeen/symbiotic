package net.scalytica.symbiotic.models

import japgolly.scalajs.react.extra.router2.RouterCtl
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.routes.SymbioticRouter
import net.scalytica.symbiotic.routes.SymbioticRouter.View
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.{Date, URIUtils}

case class User(name: String, pass: String)

object User {
  val sessionKey = "SYMBIOTIC_USER"

  def login(username: String, password: String, ctl: RouterCtl[View]): Unit = {
    for {
      res <- Ajax.post(
        url = s"${SymbioticRouter.ServerBaseURI}/login",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        ),
        data = s"""{ "username": "$username", "password": "$password" }"""
      )
    } yield {
      // TODO: Validate response and potentially redirect to some page
      if (res.status == 200) {
        log.info(s"Success ${res.status}")
        setUserCookie(username = Some(username), expire = false)
        ctl.set(SymbioticRouter.Home).unsafePerformIO()
      } else {
        log.error(s"Not correct ${res.status}")
      }
    }

  }

  def logout(ctl: RouterCtl[View]): Unit = {
    for {
      res <- Ajax.get(url = s"${SymbioticRouter.ServerBaseURI}/logout")
    } yield {
      if (res.status == 200) {
        log.info(s"Removing Cookie")
        setUserCookie(None, expire = true)
        log.info(s"Cookie is now: ${dom.document.cookie}")
        ctl.set(SymbioticRouter.Logout).unsafePerformIO()
      }
      else {
        log.error("Could not reach the server...not connected?")
      }
    }
  }

  private def setUserCookie(username: Option[String], expire: Boolean): Unit = {
    val expiresAt = if (expire) new Date(Date.now()).toISOString() else new Date(year = 9999, month = 12)
    val cookieValue = s"${username.map("user=" + _).getOrElse("")}; expires=$expiresAt; path=/"
    dom.document.cookie = s"SYMBIOTIC_USER=${URIUtils.encodeURI(cookieValue)}"
  }

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
      log.info(s"Got kvp.size=${kvp.size} with content ${kvp.mkString("\n")}")
      if (kvp.isEmpty) return false
      else kvp.size == kvp.count(t => t._1.nonEmpty && t._2.nonEmpty)
    }
  }
}
