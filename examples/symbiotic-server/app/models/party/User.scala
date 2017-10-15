package models.party

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.base._
import net.scalytica.symbiotic.api.types.PersistentType.VersionStamp
import net.scalytica.symbiotic.json.Implicits._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Representation of a registered user in the system
 */
case class User(
    id: Option[SymbioticUserId] = None,
    loginInfo: LoginInfo,
    v: Option[VersionStamp] = None,
    username: Username,
    email: Email,
    name: Option[Name] = None,
    dateOfBirth: Option[DateTime] = None,
    gender: Option[Gender] = None,
    active: Boolean = true,
    avatarUrl: Option[String] = None,
    useSocialAvatar: Boolean = true
) extends Party
    with Identity

object User {

  implicit val format: Format[User] = (
    (__ \ "id").formatNullable[SymbioticUserId] and
      (__ \ "loginInfo").format[LoginInfo] and
      (__ \ "v").formatNullable[VersionStamp] and
      (__ \ "username").format[Username] and
      (__ \ "email").format[Email] and
      (__ \ "name").formatNullable[Name] and
      (__ \ "dateOfBirth").formatNullable[DateTime] and
      (__ \ "gender").formatNullable[Gender] and
      (__ \ "active").format[Boolean] and
      (__ \ "avatarUrl").formatNullable[String] and
      (__ \ "useSocialAvatar").format[Boolean]
  )(User.apply, unlift(User.unapply))

  def fromCommonSocialProfile(csp: CommonSocialProfile): User = {
    val n = if (csp.firstName.nonEmpty || csp.lastName.nonEmpty) {
      Name(first = csp.firstName, last = csp.lastName)
    } else {
      Name(last = csp.fullName)
    }

    User(
      id = SymbioticUserId.createOpt(),
      loginInfo = csp.loginInfo,
      username = Username(csp.loginInfo.providerKey),
      // FIXME: Quite dirty.
      email = Email(csp.email.getOrElse("not_provided@scalytica.net")),
      name = Option(n),
      // remove any query params from URL
      avatarUrl = csp.avatarURL.map(_.takeWhile(_ != '?'))
    )
  }

  def updateFromCommonSocialProfile(
      csp: CommonSocialProfile,
      maybeUser: Option[User]
  ): User =
    maybeUser
      .map(
        usr =>
          usr.copy(
            loginInfo = csp.loginInfo,
            avatarUrl = csp.avatarURL
        )
      )
      .getOrElse(fromCommonSocialProfile(csp))
}

case class CreateUser(
    username: Username,
    email: Email,
    password1: Password = Password.empty,
    password2: Password = Password.empty,
    name: Option[Name] = None,
    dateOfBirth: Option[DateTime] = None,
    gender: Option[Gender] = None
) {

  def toUser(id: Option[SymbioticUserId], loginInfo: LoginInfo): User =
    User(
      id = id,
      loginInfo = loginInfo,
      username = username,
      email = email,
      name = name,
      dateOfBirth = dateOfBirth,
      gender = gender
    )
}

object CreateUser {
  implicit val formats: Format[CreateUser] = Json.format[CreateUser]
}
