package services.party

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.base.{SymbioticUserId, Username}
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import models.party.User
import repository.UserRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(
    implicit ec: ExecutionContext,
    repository: UserRepository
) extends IdentityService[User] {

  def save(user: User): Future[Either[String, SymbioticUserId]] =
    repository.save(user)

  def findById(id: UserId): Future[Option[User]] = repository.findById(id)

  def findByUsername(username: Username): Future[Option[User]] =
    repository.findByUsername(username)

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    repository.findByLoginInfo(loginInfo)

}
