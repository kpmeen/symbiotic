package services.party

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.base.Username
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import models.party.User
import net.scalytica.symbiotic.core.SuccessOrFailure
import repository.mongodb.UserRepository

import scala.concurrent.Future

@Singleton
class UserService @Inject()(val repository: UserRepository)
    extends IdentityService[User] {

  def save(user: User): SuccessOrFailure = repository.save(user)

  def findById(id: UserId): Option[User] = repository.findById(id)

  def findByUsername(username: Username): Option[User] =
    repository.findByUsername(username)

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    Future.successful(repository.findByLoginInfo(loginInfo))
}
