/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.core.session

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra.router._
import net.scalytica.symbiotic.models.AuthToken
import net.scalytica.symbiotic.models.party.User
import net.scalytica.symbiotic.routing.SymbioticRouter.{
  Home,
  Login,
  SocialAuthCallback,
  View
}
import org.scalajs.dom.ext.LocalStorage
import net.scalytica.symbiotic.logger.log
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object Session {

  val usernameKey  = "username"
  val userIdKey    = "uid"
  val authTokenKey = "authToken"

  def clear(): Unit = LocalStorage.clear()

  def init(authToken: AuthToken): Unit = {
    // Need to set the authToken before trying to get the current user.
    // Otherwise the service call will fail with an Unauthorized because
    // the token couldn't be found when setting up the XmlHttpRequest.
    LocalStorage.update(authTokenKey, authToken.token)
    User.currentUser.foreach {
      case Right(usr) =>
        usr.id.foreach(uid => LocalStorage.update(userIdKey, uid))
        LocalStorage.update(usernameKey, usr.username)
      case Left(err) =>
        log.warn("There was a problem fetching the current user")
    }
  }

  def authCodeReceived(
      acb: SocialAuthCallback,
      ctl: RouterCtl[View]
  ): Callback =
    Callback {
      log.debug(acb.queryParams)
      if (acb.queryParams.contains("&code=") || acb.queryParams.contains(
            "?code="
          )) {
        log.debug("Query parameters with 'code' key found...")
        val providerAndParams = acb.queryParams.split('?')
        User
          .authenticate(
            providerAndParams(0).stripPrefix("/"),
            Option(providerAndParams(1)).map(s => s"?$s")
          )
          .map { res =>
            if (res) ctl.set(Home).toIO.unsafePerformIO()
            else ctl.set(Login).toIO.unsafePerformIO()
          }
      } else {
        log.debug("No 'code' query parameter found. Redirecting to Login")
        ctl.set(Login).toIO.unsafePerformIO()
      }
    }

  def token: Option[AuthToken] =
    LocalStorage(authTokenKey).map(AuthToken.apply)

  def hasToken: Boolean = token.nonEmpty

  def userId: Option[String] = LocalStorage(userIdKey)

  def username: Option[String] = LocalStorage(usernameKey)

}
