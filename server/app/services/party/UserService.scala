/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.User
import net.scalytica.symbiotic.core.SuccessOrFailure
import net.scalytica.symbiotic.persistence.UserRepository

import scala.concurrent.Future

// import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class UserService @Inject()(
    val userRepository: UserRepository
) extends IdentityService[User] {

  def save(user: User): SuccessOrFailure = userRepository.save(user)

  def findById(id: UserId): Option[User] = userRepository.findById(id)

  def findByUsername(username: Username): Option[User] =
    userRepository.findByUsername(username)

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    Future.successful {
      userRepository.findByLoginInfo(loginInfo)
    }
}
