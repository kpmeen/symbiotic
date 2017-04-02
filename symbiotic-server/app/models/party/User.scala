package models.party

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.base._
import net.scalytica.symbiotic.data.PartyBaseTypes.{Party, UserId}
import net.scalytica.symbiotic.data.PersistentType.VersionStamp
import net.scalytica.symbiotic.data.PersistentTypeConverters
import net.scalytica.symbiotic.play.json.DateTimeFormatters
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

/**
 * Representation of a registered user in the system
 */
case class User(
    id: Option[UserId] = None,
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

object User extends PersistentTypeConverters with DateTimeFormatters {
  implicit val formats: Format[User] = Json.format[User]

  def fromCommonSocialProfile(csp: CommonSocialProfile): User = {
    val n = if (csp.firstName.nonEmpty || csp.lastName.nonEmpty) {
      Name(first = csp.firstName, last = csp.lastName)
    } else {
      Name(last = csp.fullName)
    }

    User(
      id = UserId.createOpt(),
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

  def toUser(id: Option[UserId], loginInfo: LoginInfo): User =
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

object CreateUser extends DateTimeFormatters {
  implicit val formats: Format[CreateUser] = Json.format[CreateUser]
}
