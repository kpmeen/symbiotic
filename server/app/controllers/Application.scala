/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import core.security.authentication.Authenticated._
import models.base.Username
import models.party.User
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

@Singleton
class Application extends Controller {

  val logger: Logger = Logger(this.getClass)

  def login() = Action(parse.json) { implicit request =>
    validate {
      case user: User =>
        if (user.active) accessGranted(user)
        else deactivatedUser(request, user)
    }
  }

  def logout = Action { implicit request =>
    Ok.withNewSession
  }

  private def validate(grantAccess: User => Result)(implicit request: Request[JsValue]): Result = {
    val theJson = request.body
    val username = (theJson \ "username").asOpt[String]
    val password = (theJson \ "password").asOpt[String]

    validateCredentials(username.flatMap(un => Option(Username(un))), password)(grantAccess)
  }

}
