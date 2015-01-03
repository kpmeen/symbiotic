package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController

object Application extends Controller with MongoController {

  def login(username: String, password: String) = Action(parse.json) { request =>
    NotImplemented
  }

  def logout = Action { request =>
    NotImplemented
  }

}