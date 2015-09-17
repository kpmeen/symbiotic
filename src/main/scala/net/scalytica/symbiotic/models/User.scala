package net.scalytica.symbiotic.models

import japgolly.scalajs.react.extra.router2.RouterCtl
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.routes.SymbioticRouter
import net.scalytica.symbiotic.routes.SymbioticRouter.View
import net.scalytica.symbiotic.util.Cookies
import org.scalajs.dom.ext.Ajax

import scala.concurrent.ExecutionContext.Implicits.global

case class User(name: String, pass: String)

object User {
  val sessionKey = "SYMBIOTIC_USER"

  def login(username: String, password: String, ctl: RouterCtl[View]): Unit =
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
      if (res.status == 200) {
        Cookies.set(sessionKey, Map("username" -> username))
        ctl.set(SymbioticRouter.Home(SymbioticRouter.TestOrgId)).unsafePerformIO()
      } else {
        log.error(s"Not correct ${res.status}")
      }
    }

  def logout(ctl: RouterCtl[View]): Unit =
    for {
      res <- Ajax.get(url = s"${SymbioticRouter.ServerBaseURI}/logout")
    } yield {
      if (res.status == 200) {
        Cookies.remove(sessionKey)
        ctl.set(SymbioticRouter.Login).unsafePerformIO()
      }
      else {
        log.error("Could not reach the server...not connected?")
      }
    }

  /**
   * @return If no sessionKey cookie is found, returns false
   */
  def isLoggedIn: Boolean = Cookies.get(sessionKey).exists { mc =>
    if (mc.isEmpty) return false
    val kvp = mc.split("=").tail match {
      case Array(k, v) => (k, v)
    }
    kvp._1.nonEmpty && kvp._2.nonEmpty
  }
}
