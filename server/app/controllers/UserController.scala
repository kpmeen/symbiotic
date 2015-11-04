/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream
import javax.inject.Singleton

import core.security.authentication.Authenticated
import models.party.PartyBaseTypes.UserId
import models.party.{Avatar, User}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import services.party.{AvatarService, UserService}

@Singleton
class UserController extends SymbioticController with FileStreaming {

  val logger = Logger("UserController")

  /**
   * Will try to get the User with the provided UserId
   */
  def get(uid: String) = Authenticated { implicit request =>
    UserId.asOptId(uid).map { i =>
      UserService.findById(i).map(u => Ok(Json.toJson(u))).getOrElse(NotFound)
    }.getOrElse(BadIdFormatResponse)
  }

  /**
   * Add a new User
   */
  def add = Authenticated(parse.json) { implicit request =>
    Json.fromJson[User](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(u) =>
        UserService.findByUsername(u.username).map(_ =>
          Conflict(Json.obj("msg" -> s"user ${u.username} already exists"))).getOrElse {
          UserService.save(u.copy(id = UserId.createOpt()))
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
        userId.map { i =>
          UserService.findById(i).map { u =>
            val usr = user.copy(_id = u._id, id = u.id, password = u.password)
            UserService.save(usr)
            Ok(Json.obj("msg" -> "sucessfully updated user"))
          }.getOrElse(NotFound)
        }.getOrElse(BadIdFormatResponse)
    }
  }

  def setAvatar(uid: String) = Authenticated(parse.multipartFormData) { implicit request =>
    val avatar = request.body.files.headOption.map { tmp =>
      Avatar(uid, tmp.contentType, Option(new FileInputStream(tmp.ref.file)))
    }
    avatar.fold(BadRequest(Json.obj("msg" -> "No avatar image attached"))) { a =>
      logger.debug(s"Going to save avatar $a for user $uid")
      AvatarService.save(a).fold(InternalServerError(Json.obj("msg" -> "bad things"))) { fid =>
        Ok(Json.obj("msg" -> s"Saved file with Id $fid"))
      }
    }
  }

  def getAvatar(uid: String) = Authenticated { implicit request =>
    serve(AvatarService.get(uid))
  }

}
