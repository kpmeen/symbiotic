package controllers

import play.api.mvc._

import scala.concurrent.Future

object Application extends Controller {

  def login(username: String, password: String) = Action.async(parse.json) { request =>
    Future.successful(NotImplemented)
  }

  def logout = Action.async { request =>
    Future.successful(NotImplemented)
  }

}