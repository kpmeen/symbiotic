/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import models.parties.{User, UserId}
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}

object UserController extends Controller {

  val logger = Logger("UserController")

  /**
   * Will try to get the User with the provided UserId
   */
  def get(uid: String) = Action { implicit request =>
    UserId.asOptId(uid).map(i =>
      User.findById(i).map(u => Ok(Json.toJson(u))).getOrElse(NotFound)
    ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal user ID format")))
  }

  /**
   * Add a new User
   */
  def add() = Action(parse.json) { implicit request =>
    Json.fromJson[User](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(JsError(jserr)))
      case Right(u) =>
        User.save(u)
        Created(Json.obj("msg" -> "successfully created new user"))
    }

  }

  /**
   * Update the User with the given UserId
   */
  def update(uid: String) = Action(parse.json) { implicit request =>
    Json.fromJson[User](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(JsError(jserr)))
      case Right(user) =>
        val userId = UserId.asOptId(uid)
        userId.map(i =>
          User.findById(i).map { u =>
            val usr = user.copy(id = userId, password = u.password)
            User.save(usr)
            Ok(Json.obj("msg" -> "sucessfully updated user"))
          }.getOrElse(NotFound)
        ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal user ID format")))
    }
  }

}
