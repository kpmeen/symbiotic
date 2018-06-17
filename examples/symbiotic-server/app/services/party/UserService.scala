package services.party

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import core.DocManContext
import models.base.{SymbioticUserId, Username}
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import models.party.User
import net.scalytica.symbiotic.api.SymbioticResults.{Ko, Ok}
import net.scalytica.symbiotic.core.DocManagementService
import play.api.Logger
import repository.UserRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(
    implicit ec: ExecutionContext,
    repository: UserRepository,
    dmanService: DocManagementService
) extends IdentityService[User] {

  private[this] val log = Logger(getClass)

  def save(user: User): Future[Either[String, SymbioticUserId]] = {
    repository.save(user).flatMap {
      case Right(uid) =>
        log.debug(s"Attempting to initialize root folder for $uid")
        dmanService.createRootIfNotExists(DocManContext(uid), ec).map {
          case Ok(_) => Right(uid)
          case _: Ko => Left(s"failed initializing root folder for $uid")
        }

      case Left(msg) => Future.successful(Left(msg))
    }
  }

  def findById(id: UserId): Future[Option[User]] = repository.findById(id)

  def findByUsername(username: Username): Future[Option[User]] =
    repository.findByUsername(username)

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    repository.findByLoginInfo(loginInfo)

}
