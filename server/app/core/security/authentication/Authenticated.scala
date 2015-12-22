/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.security.authentication

import core.security.authentication.Crypto._
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.User
import org.apache.commons.codec.binary.Base64
import org.bson.types.ObjectId
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.{Logger, Play}
import services.party.UserService

import scala.concurrent.Future
import scala.concurrent.Future.{successful => resolve}

/**
 * Whenever a user successfully passes through the Authenticated Action, the request will be
 * transformed into a UserRequest. And enriched with the additional data defined.
 */
case class UserRequest[A](uname: Username, sessionId: String, request: Request[A]) extends WrappedRequest[A](request) {

  lazy val currentUser: User = UserService.findByUsername(uname).get

  lazy val currentUserId: UserId = currentUser.id.get

}

/**
 * An action that will verify the authenticity of a users session cookie or basic authentication credentials.
 *
 * This implementation also includes convenience functions for authentication related operations.
 */
object Authenticated extends ActionBuilder[UserRequest] {

  val logger = Logger(this.getClass)

  val Cookie_Username = "username"
  val Cookie_SessionId = "sessionId"

  val InactivityTimeout =
    Play.current.configuration.getInt("symbiotic.inactivity.timeout").getOrElse(60) * 60000 // scalastyle:ignore

  private lazy val BasicSt = "basic"

  case class Credentials(usr: Username, pass: String)

  private sealed trait AuthMethod

  private case class CookieAuth(usr: Username) extends AuthMethod

  private case class BaseAuth(creds: Credentials) extends AuthMethod

  override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] =
    authenticate(request).map {
      case Left(c: Credentials) =>
        val sessionId = generateSessionId
        block(new UserRequest[A](c.usr, sessionId, request)).map((result: Result) => result)
      case Right(auid) =>
        block(new UserRequest[A](auid, request.session.get(Cookie_SessionId).get, request)).map((result: Result) =>
          result.withSession(request.session))
    }.getOrElse(resolve(unauthorized(request, unauthorizedMessage(request.uri))))

  /**
   * This is a private function for checking the existence of either the appropriate authentication cookie,
   * or for basic auth information.
   */
  private[security] def authenticate(request: RequestHeader): Option[Either[Credentials, Username]] = {
    val username = request.session.get(Cookie_Username)
    val sessionId = request.session.get(Cookie_SessionId)
    username.flatMap { uname =>
      sessionId.map { _ =>
        Right(Username(uname))
      }
    }.orElse {
      // Let's see if some basic auth headers were present in the request.
      request.headers.get("authorization").flatMap(basicAuth => {
        logger.warn(s"BasicAuth: $basicAuth")
        decodeBasicAuth(basicAuth).flatMap(c =>
          UserService.findByUsername(c.usr).flatMap(usr => Some(Left(c))))
      })
    }
  }

  /**
   * Function for decoding content of the basic authentication information
   */
  private def decodeBasicAuth(auth: String): Option[Credentials] = {

    def parseCredentials(arg: String): Option[Credentials] =
      if (arg.split(":").length >= 2) {
        // account for ":" in passwords
        val creds = arg.splitAt(arg.indexOf(":"))
        val username = creds._1
        val password = creds._2.drop(1)
        Some(Credentials(Username(username), password))
      } else {
        None
      }

    if (auth.length >= BasicSt.length) {
      val basicReqSt = auth.substring(0, BasicSt.length)
      if (basicReqSt.toLowerCase == BasicSt) {
        val basicAuthSt = auth.replaceFirst(basicReqSt, "")
        parseCredentials(new String(Base64.decodeBase64(basicAuthSt), "UTF-8"))
      } else None
    } else None
  }

  /**
   * Will try to validate and verify the user credentials provided.
   */
  def validateCredentials(
    uname: Option[Username],
    password: Option[String]
  )(grantAccess: User => Result)(implicit request: Request[JsValue]): Result = {
    uname.map(un => UserService.findByUsername(un).fold(invalidCredentials(request))(user => {
      password.fold(invalidCredentials(request))(p => {
        if (!isValidPassword(p, user.password)) {
          invalidCredentials(request)
        } else {
          grantAccess(user)
        }
      })
    })).getOrElse(invalidCredentials(request))
  }

  /**
   * Defines the default message for unauthorised access attempts with invalid credentials.
   */
  def invalidCredentials(request: Request[JsValue]): Result = {
    logger.info("Login attempted with invalid credentials")
    unauthorized(request, s"Invalid credentials")
  }

  /**
   * Defines the default message for unauthorised access attempts
   */
  def unauthorizedMessage(uri: String) = s"Service $uri requires authentication"

  /**
   * Logs and returns information about unauthorized access attempts.
   *
   * If the request contains the `X-Requested-With` header _and_ that value is `XMLHttpRequest`, we know
   * there was a form based login attempt. If the header and value does not match the mentioned criteria,
   * we are dealing with basic authentication. In which case we need to ensure the response has the
   * `WWW-Authenticate` header set with a proper `Basic realm` value.
   */
  def unauthorized(request: RequestHeader, message: String): Result = {
    logger.warn(unauthorizedMessage(request.uri))
    request.headers.get("Authorization")
      .map(reqWith => unauthorizedBasicAuthResult(request, message))
      .getOrElse(unauthorizedResult(message))
      .withNewSession
  }

  /**
   * The standard unauthorized result.
   */
  private def unauthorizedResult(message: String): Result = {
    Results.Unauthorized(
      Json.obj(
        "code" -> Status.UNAUTHORIZED,
        "reason" -> "Access denied",
        "message" -> message
      )
    )
  }

  /**
   * In case the basic authorization fails, return the default unauthorized result with `WWW-Authenticate` header.
   */
  private def unauthorizedBasicAuthResult(request: RequestHeader, message: String): Result = {
    unauthorizedResult(message).withHeaders(("WWW-Authenticate", s"""Basic realm="${request.uri}""""))
  }

  /**
   * Grants access to the given user by returning a session cookie with authentication tokens.
   *
   * Only really useful for form based login.
   */
  def accessGranted(user: User): Result = {
    user.id.map { uid =>
      Results.Ok(Json.obj("value" -> uid.value)).withSession(
        Cookie_Username -> user.username.value,
        Cookie_SessionId -> generateSessionId
      )
    }.getOrElse {
      Results.InternalServerError(Json.obj("message" -> "It seems access was attempted by a User with no UserId"))
    }
  }

  /**
   * Defines the default message for access attempts by deactivated users
   */
  def deactivatedUser(request: Request[JsValue], user: User): Result = {
    logger.info(s"Login attempted by deactivated user - ${user.username.value}")
    unauthorized(request, "The user is deactivated")
  }

  /**
   * Returns a new, unauthenticated, session
   */
  def invalidateLogin(implicit request: Request[Any]): Result = {
    Results.NoContent.withNewSession
  }

  def generateSessionId = new ObjectId().toHexString
}
