/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package controllers

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import play.api.Logger
import services.party.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import core.lib.{Failure, Success}
import models.party.PartyBaseTypes.UserId
import models.party.{CreateUser, User}
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, JsError, Json}
import play.api.mvc.{Result, Request, Action}

import scala.concurrent.Future

@Singleton
class SignUpController @Inject() (
    val messagesApi: MessagesApi,
    val env: Environment[User, JWTAuthenticator],
    val userService: UserService,
    avatarService: AvatarService,
    authInfoRepository: AuthInfoRepository,
    passwordHasher: PasswordHasher
) extends SymbioticController {

  private val log = Logger(this.getClass)

  /**
   * Allows a User to sign up for the service
   */
  def signUp = Action.async(parse.json) { implicit request =>
    Json.fromJson[CreateUser](request.body).asEither match {
      case Left(jserr) => Future.successful(BadRequest(JsError.toJson(JsError(jserr))))
      case Right(u) =>
        val loginInfo = LoginInfo(CredentialsProvider.ID, u.username.value)
        userService.retrieve(loginInfo).flatMap[Result] {
          case Some(user) =>
            Future.successful(Conflict(Json.obj("msg" -> s"user ${u.username} already exists")))

          case None =>
            val authInfo = passwordHasher.hash(u.password.value)
            val usr = u.toUser(UserId.createOpt(), loginInfo)
            avatarService.retrieveURL(usr.email.adr).flatMap { maybeAvatarUrl =>
              saveUser(usr.copy(avatarUrl = maybeAvatarUrl), loginInfo, authInfo)
            }.fallbackTo(saveUser(usr, loginInfo, authInfo))
        }
    }
  }

  def saveUser(usr: User, loginInfo: LoginInfo, authInfo: AuthInfo)(implicit request: Request[JsValue]): Future[Result] =
    userService.save(usr) match {
      case s: Success =>
        for {
          authInfo <- authInfoRepository.add(loginInfo, authInfo)
          authenticator <- env.authenticatorService.create(loginInfo)
          value <- env.authenticatorService.init(authenticator)
          result <- env.authenticatorService.embed(value, Created(Json.obj("msg" -> s"User was created")))
        } yield {
          env.eventBus.publish(SignUpEvent(usr, request, request2Messages))
          env.eventBus.publish(LoginEvent(usr, request, request2Messages))
          result
        }

      case Failure(msg) =>
        Future.successful(InternalServerError(Json.obj("msg" -> msg)))
    }
}
