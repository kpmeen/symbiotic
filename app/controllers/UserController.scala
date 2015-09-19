/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import core.security.authentication.Authenticated
import models.party.PartyBaseTypes.UserId
import models.party.User
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import play.api.mvc._

@Singleton
class UserController extends Controller {

  val logger = Logger("UserController")

  /**
   * Will try to get the User with the provided UserId
   */
  def get(uid: String) = Authenticated { implicit request =>
    UserId.asOptId(uid).map(i =>
      User.findById(i).map(u => Ok(Json.toJson(u))).getOrElse(NotFound)
    ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal ID format")))
  }

  /**
   * Add a new User
   */
  def add() = Authenticated(parse.json) { implicit request =>
    Json.fromJson[User](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(u) =>
        User.findByUsername(u.username).map(_ =>
          Conflict(Json.obj("msg" -> s"user ${u.username} already exists"))
        ).getOrElse {
          User.save(u.copy(id = UserId.createOpt()))
          Created(Json.obj("msg" -> "successfully created new user"))
        }
    }

  }

  /**
   * Update the User with the given UserId
   */
  def update(uid: String) = Authenticated(parse.json) { implicit request =>
    Json.fromJson[User](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
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
