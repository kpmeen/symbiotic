package controllers

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import models.base.{SymbioticUserId, Username}
import org.joda.time.DateTime
import services.party.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import core.security.authentication.JWTEnvironment
import models.party.{CreateUser, User}
import net.scalytica.symbiotic.json.Implicits._
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{ControllerComponents, Request, Result}

import scala.concurrent.Future

@Singleton
class RegistrationController @Inject()(
    val controllerComponents: ControllerComponents,
    silhouette: Silhouette[JWTEnvironment],
    userService: UserService,
    avatarService: AvatarService,
    authInfoRepository: AuthInfoRepository,
    passwordHasher: PasswordHasher
) extends SymbioticController {

  /**
   * Allows a User to sign up for the service
   */
  def register = Action.async(parse.json) { implicit request =>
    Json.fromJson[CreateUser](request.body).asEither match {
      case Left(jserr) =>
        Future.successful(BadRequest(JsError.toJson(JsError(jserr))))

      case Right(u) =>
        val loginInfo = LoginInfo(CredentialsProvider.ID, u.username.value)
        userService.retrieve(loginInfo).flatMap[Result] {
          case Some(_) =>
            Future.successful {
              Conflict(Json.obj("msg" -> s"user ${u.username} already exists"))
            }

          case None =>
            val maybeUid = SymbioticUserId.createOpt()
            val authInfo = passwordHasher.hash(u.password1.value)
            val usr      = u.toUser(maybeUid, loginInfo)
            avatarService
              .retrieveURL(usr.email.adr)
              .flatMap { maybeAvatarUrl =>
                saveUser(
                  usr.copy(avatarUrl = maybeAvatarUrl),
                  loginInfo,
                  authInfo
                )
              }
              .fallbackTo(saveUser(usr, loginInfo, authInfo))
        }
    }
  }

  def saveUser(
      usr: User,
      loginInfo: LoginInfo,
      authInfo: AuthInfo
  )(implicit request: Request[JsValue]): Future[Result] =
    userService.save(usr).flatMap {
      case Right(_) =>
        for {
          _           <- authInfoRepository.add(loginInfo, authInfo)
          authService <- silhouette.env.authenticatorService.create(loginInfo)
          value       <- silhouette.env.authenticatorService.init(authService)
        } yield {
          silhouette.env.eventBus.publish(SignUpEvent(usr, request))
          silhouette.env.eventBus.publish(LoginEvent(usr, request))
          Created(
            Json.obj(
              "token" -> value,
              "expiresOn" -> Json.toJson[DateTime](
                authService.expirationDateTime
              )
            )
          )
        }

      case Left(msg) =>
        Future.successful(InternalServerError(Json.obj("msg" -> msg)))
    }

  /**
   * Returns 406 - NotAcceptable if the username exists. Otherwise 200 Ok.
   */
  def validateUsername(uname: String) = Action.async { implicit request =>
    userService
      .findByUsername(Username(uname))
      .map(maybeUser => maybeUser.map(_ => Conflict).getOrElse(Ok))
  }
}
