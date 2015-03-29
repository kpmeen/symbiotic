/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import models.base.Username
import models.parties.User
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import core.security.authentication.Authenticated._

import scala.concurrent.Future

object Application extends Controller {

  val logger: Logger = Logger(this.getClass)

  def login(username: String, password: String) = Action.async(parse.json) { implicit request =>
    Future.successful {
      validate {
        case user: User => accessGranted(user)
        //        if (user.active) {
        //          accessGranted(user)
        //        } else {
        //          deactivatedUser(request, user)
        //        }
      }
    }
  }

  def logout = Action.async { request =>
    Future.successful(NotImplemented)
  }

  private def validate(grantAccess: User => Result)(implicit request: Request[JsValue]): Result = {
    val theJson = request.body
    val username = (theJson \ "username").asOpt[String]
    val password = (theJson \ "password").asOpt[String]

    validateCredentials(username.flatMap(un => Option(Username(un))), password)(grantAccess)
  }

}