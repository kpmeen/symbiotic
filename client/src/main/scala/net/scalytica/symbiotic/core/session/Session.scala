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
import net.scalytica.symbiotic.logger.log
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object Session {

  val usernameKey = "username"
  val userIdKey = "uid"
  val authTokenKey = "authToken"

  val storage = localStorage

  def clear(): Unit = storage.clear()

  def init(authToken: AuthToken): Unit = {
    // Need to set the authToken before trying to get the current user.
    // Otherwise the service call will fail with an Unauthorized because
    // the token couldn't be found when setting up the XmlHttpRequest.
    storage.setItem(authTokenKey, authToken.token)
    User.currentUser.foreach {
      case Right(usr) =>
        usr.id.foreach(uid => storage.setItem(userIdKey, uid))
        storage.setItem(usernameKey, usr.username)
      case Left(err) =>
        log.warn("There was a problem fetching the current user")
    }
  }

  def authCodeReceived(acb: SocialAuthCallback, ctl: RouterCtl[View]): Callback =
    Callback {
      if (acb.queryParams.contains("&code=")) {
        log.debug("Query parameters with 'code' key found...")
        User.authenticate(
          "google",
          Some(acb.queryParams)
        ).map { res =>
          if (res) ctl.set(Home).toIO.unsafePerformIO()
          else ctl.set(Login).toIO.unsafePerformIO()
        }
      } else {
        log.debug("No 'code' query parameter found. Redirecting to Login")
        ctl.set(Login).toIO.unsafePerformIO()
      }
    }

  def token: Option[AuthToken] = Option(storage.getItem(authTokenKey)).map(AuthToken.apply)

  def hasToken: Boolean = token.nonEmpty

  def userId: Option[String] = Option(storage.getItem(userIdKey))

  def username: Option[String] = Option(storage.getItem(usernameKey))

}
