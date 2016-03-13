/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import core.converters.DateTimeConverters
import models.base.PersistentType.VersionStamp
import models.base._
import models.party.PartyBaseTypes.{Party, UserId}
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
) extends Party with Identity

object User extends PersistentTypeConverters with DateTimeConverters {
  implicit val formats: Format[User] = Json.format[User]

  def fromCommonSocialProfile(csp: CommonSocialProfile): User = {
    val n = {
      if (csp.firstName.nonEmpty || csp.lastName.nonEmpty) Name(first = csp.firstName, last = csp.lastName)
      else Name(last = csp.fullName)
    }

    User(
      id = UserId.createOpt(),
      loginInfo = csp.loginInfo,
      username = Username(csp.loginInfo.providerKey),
      email = Email(csp.email.getOrElse("not_provided@scalytica.net")), // FIXME: Call a service to fetch from the social provider API.
      name = Option(n),
      avatarUrl = csp.avatarURL.map(_.takeWhile(_ != '?')) // remove any query params from URL
    )
  }

  def updateFromCommonSocialProfile(csp: CommonSocialProfile, maybeUser: Option[User]): User =
    maybeUser.map(usr =>
      usr.copy(
        loginInfo = csp.loginInfo,
        avatarUrl = csp.avatarURL
      )).getOrElse(fromCommonSocialProfile(csp))
}

case class CreateUser(
    username: Username,
    email: Email,
    password: Password = Password.empty,
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

object CreateUser extends DateTimeConverters {
  implicit val formats: Format[CreateUser] = Json.format[CreateUser]
}