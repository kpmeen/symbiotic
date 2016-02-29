/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.core.session

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra.router._
import net.scalytica.symbiotic.models.{AuthToken, User}
import net.scalytica.symbiotic.routing.SymbioticRouter.{Home, Login, SocialAuthCallback, View}
import org.scalajs.dom.localStorage

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object Session {

  val UserCookie = "SYMBIOTIC_USER"

  val OAuth2StateCookie = "OAuth2State"

  val usernameKey = "username"
  val userIdKey = "uid"

  val storage = localStorage

  def clear(): Unit = storage.removeItem("authToken")

  def init(authToken: AuthToken): Unit = {
    println(s"Got authToken $authToken")
    storage.setItem("authToken", authToken.token)
  }

  def authCodeReceived(acb: SocialAuthCallback, ctl: RouterCtl[View]): Callback = {
    Callback {
      if (acb.queryParams.contains("&code=")) {
        println("Found a query string")
        User.authenticate(
          "google",
          Some(acb.queryParams)
        ).map { res =>
          if (res) ctl.set(Home).toIO.unsafePerformIO()
          else ctl.set(Login).toIO.unsafePerformIO()
        }
      } else {
        println("No query parameters found. Redirecting to Login")
        ctl.set(Login).toIO.unsafePerformIO()
      }
    }
  }

  def token: Option[AuthToken] = Option(storage.getItem("authToken")).map(AuthToken.apply)

  def hasToken: Boolean = token.nonEmpty

  def userId: Option[String] = Cookies.valueOf(UserCookie, userIdKey) // TODO: Read value from service

  def username: Option[String] = Cookies.valueOf(UserCookie, usernameKey) // TODO: Read value from service

}
