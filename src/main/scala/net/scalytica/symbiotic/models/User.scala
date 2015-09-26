package net.scalytica.symbiotic.models

import japgolly.scalajs.react.extra.router2.RouterCtl
import net.scalytica.symbiotic.core.session.Session._
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.routes.SymbioticRouter
import net.scalytica.symbiotic.routes.SymbioticRouter.View
import net.scalytica.symbiotic.util.Cookies
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.Ajax

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js

case class Credentials(uname: String, pass: String)

case class User(v: Option[VersionStamp] = None,
  id: Option[String] = None,
  username: Username,
  email: Email,
  password: String,
  name: Option[Name] = None,
  dateOfBirth: Option[js.Date] = None,
  gender: Option[String] = None,
  active: Boolean = true)

object User {
  def login(creds: Credentials, ctl: RouterCtl[View]): Future[XMLHttpRequest] =
    Ajax.post(
      url = s"${SymbioticRouter.ServerBaseURI}/login",
      headers = Map(
        "Accept" -> "application/json",
        "Content-Type" -> "application/json"
      ),
      data = s"""{ "username": "${creds.uname}", "password": "${creds.pass}" }"""
    )

  def logout(ctl: RouterCtl[View]): Unit =
    Ajax.get(url = s"${SymbioticRouter.ServerBaseURI}/logout").map { res =>
      // We don't care what the response status is...we'll remove the cookie anyway
      Cookies.remove(sessionKey)
      ctl.set(SymbioticRouter.Login).unsafePerformIO()
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

  def getUser: User = ???
}
