/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Silhouette
import core.lib.{Failure, Success}
import core.lib.ImageTransformer.resizeImage
import core.security.authentication.JWTEnvironment
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.{Avatar, User}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsError, Json}
import services.party.{AvatarService, UserService}

import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class UserController @Inject() (
    val messagesApi: MessagesApi,
    val silhouette: Silhouette[JWTEnvironment],
    val userService: UserService,
    val avatarService: AvatarService
) extends SymbioticController with FileStreaming {

  import silhouette.{SecuredAction, UserAwareAction}

  val avatarHeight = 120
  val avatarWidth = 120
  private val log = Logger(this.getClass)

  /**
   * Try to fetch the current user from the "session"
   */
  def current = UserAwareAction { implicit request =>
    request.identity match {
      case Some(usr) => Ok(Json.toJson(usr))
      case _ => Unauthorized
    }
  }

  /**
   * Will try to get the User with the provided UserId
   */
  def get(uid: String) = SecuredAction { implicit request =>
    UserId.asOptId(uid).map { i =>
      userService.findById(i).map(u => Ok(Json.toJson(u))).getOrElse(NotFound)
    }.getOrElse(badIdFormatResponse)
  }

  /**
   * Find a user by username
   */
  def findByUsername(uname: String) = SecuredAction { implicit request =>
    userService.findByUsername(Username(uname))
      .map(u => Ok(Json.toJson(u)))
      .getOrElse(NotFound)
  }

  /**
   * Update the User with the given UserId
   */
  def update(uid: String) = SecuredAction(parse.json) { implicit request =>
    Json.fromJson[User](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(user) =>
        val userId = UserId.asOptId(uid)
        userId.map { i =>
          userService.findById(i).map { u =>
            val usr = user.copy(id = u.id)
            userService.save(usr) match {
              case s: Success => Ok(Json.obj("msg" -> s"User was updated"))
              case Failure(msg) => InternalServerError(Json.obj("msg" -> msg))
            }
          }.getOrElse(NotFound)
        }.getOrElse(badIdFormatResponse)
    }
  }

  /**
   * Upload a new avatar image
   */
  def uploadAvatar(
    uid: String
  ) = SecuredAction(parse.multipartFormData) { implicit request =>
    request.body.files.headOption.map { tmp =>
      val resized = resizeImage(tmp.ref.file, avatarWidth, avatarHeight).getOrElse(tmp.ref.file) // scalastyle:ignore
      val a = Avatar(uid, tmp.contentType, Option(new FileInputStream(resized)))
      log.debug(s"Going to save avatar $a for user $uid")
      val res = avatarService.save(a).fold(
        InternalServerError(Json.obj("msg" -> "bad things"))
      ) { fid =>
          // TODO: Update user and set a flag "hasCustomAvatar" or something
          Ok(Json.obj("msg" -> s"Saved file with Id $fid"))
        }
      resized.delete()
      res
    }.getOrElse(BadRequest(Json.obj("msg" -> "No avatar image attached")))
  }

  /**
   * Fetch the avatar image for the given UserId
   */
  def downloadAvatar(uid: String) = SecuredAction { implicit request =>
    serve(avatarService.get(uid))
  }

}
