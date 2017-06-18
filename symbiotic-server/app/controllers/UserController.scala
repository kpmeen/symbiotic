package controllers

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Silhouette
import core.lib.ImageTransformer.resizeImage
import core.security.authentication.JWTEnvironment
import models.base.{SymbioticUserId, Username}
import models.party._
import net.scalytica.symbiotic.api.types.{Failure, Success}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.party.{AvatarService, UserService}

import scala.concurrent.Future

@Singleton
class UserController @Inject()(
    messagesApi: MessagesApi,
    silhouette: Silhouette[JWTEnvironment],
    userService: UserService,
    avatarService: AvatarService
) extends SymbioticController
    with FileStreaming {

  import silhouette.{SecuredAction, UserAwareAction}

  val avatarHeight = 120
  val avatarWidth  = 120
  private val log  = Logger(this.getClass)

  /**
   * Try to fetch the current user from the "session"
   */
  def current = UserAwareAction { implicit request =>
    request.identity match {
      case Some(usr: User) => Ok(Json.toJson(usr))
      case _               => Unauthorized
    }
  }

  /**
   * Will try to get the User with the provided UserId
   */
  def get(uid: String) = SecuredAction.async { implicit request =>
    SymbioticUserId
      .asOptId(uid)
      .map { i =>
        userService
          .findById(i)
          .map(_.map(u => Ok(Json.toJson(u))).getOrElse(NotFound))
      }
      .getOrElse(Future.successful(badIdFormatResponse))
  }

  /**
   * Find a user by username
   */
  def findByUsername(uname: String) = SecuredAction.async { implicit request =>
    userService
      .findByUsername(Username(uname))
      .map(_.map(u => Ok(Json.toJson(u))).getOrElse(NotFound))
  }

  /**
   * Update the User with the given UserId
   */
  def update(uid: String) =
    SecuredAction.async(parse.json) { implicit request =>
      Json.fromJson[User](request.body) match {
        case jserr: JsError =>
          Future.successful(BadRequest(JsError.toJson(jserr)))

        case JsSuccess(user, jsPath) =>
          SymbioticUserId
            .asOptId(uid)
            .map { i =>
              userService.findById(i).flatMap { maybeUser =>
                maybeUser.map { u =>
                  val usr = user.copy(id = u.id)
                  userService.save(usr).map {
                    case s: Success =>
                      Ok(Json.obj("msg" -> s"User was updated"))
                    case Failure(msg) =>
                      InternalServerError(Json.obj("msg" -> msg))
                  }
                }.getOrElse(Future.successful(NotFound))
              }
            }
            .getOrElse(Future.successful(badIdFormatResponse))
      }
    }

  /**
   * Upload a new avatar image
   */
  def uploadAvatar(
      uid: String
  ) = SecuredAction.async(parse.multipartFormData) { implicit request =>
    request.body.files.headOption.map { tmp =>
      val suid = SymbioticUserId.asId(uid)
      val resized = resizeImage(tmp.ref.file, avatarWidth, avatarHeight)
        .getOrElse(tmp.ref.file)
      val a =
        Avatar(suid, tmp.contentType, Option(FileIO.fromPath(resized.toPath)))
      log.debug(s"Going to save avatar $a for user $suid")
      val res = avatarService.save(a).map { maybeUUID =>
        maybeUUID.fold(
          InternalServerError(Json.obj("msg" -> "bad things"))
        ) { fid =>
          // TODO: Update user and set a flag "hasCustomAvatar" or something
          Ok(Json.obj("msg" -> s"Saved file with Id $fid"))
        }
      }
      resized.delete()
      res
    }.getOrElse {
      Future.successful(
        BadRequest(Json.obj("msg" -> "No avatar image attached"))
      )
    }
  }

  /**
   * Fetch the avatar image for the given UserId
   */
  def downloadAvatar(uid: String) = SecuredAction.async { implicit request =>
    SymbioticUserId
      .asOptId(uid)
      .map(i => avatarService.get(i).map(ma => serve(ma)))
      .getOrElse(Future.successful(badIdFormatResponse))
  }

}
