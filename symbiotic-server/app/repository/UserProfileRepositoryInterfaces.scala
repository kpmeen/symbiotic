package repository

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.base.{SymbioticUserId, Username}
import models.party.{Avatar, User}
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId

import scala.concurrent.{ExecutionContext, Future}

trait UserRepository {

  def save(
      user: User
  )(implicit ec: ExecutionContext): Future[Either[String, SymbioticUserId]]

  def findById(id: UserId)(implicit ec: ExecutionContext): Future[Option[User]]

  def findByUsername(
      username: Username
  )(implicit ec: ExecutionContext): Future[Option[User]]

  def findByLoginInfo(
      loginInfo: LoginInfo
  )(implicit ec: ExecutionContext): Future[Option[User]]

}

trait AvatarRepository {

  /**
   * Saves a new Avatar for the User specified in the metadata.
   * Only 1 avatar image per user will be kept, so this method will ensure
   * that old avatar images are cleaned up after adding the new one.
   *
   * @param a the Avatar to save
   * @return an Option that will contain the UUID of the added avatar if successful
   */
  def save(a: Avatar)(implicit ec: ExecutionContext): Future[Option[UUID]]

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param uid UserId
   * @return Option[File]
   */
  def get(uid: UserId)(implicit ec: ExecutionContext): Future[Option[Avatar]]

  /**
   * Removes _all_ avatar images where filename equals the uid
   *
   * @param uid UserId to remove avatar images for
   */
  def remove(uid: UserId)(implicit ec: ExecutionContext): Future[Unit]

  /**
   *
   * @param uid UserId to remove files for.
   * @param ids a collection of the UUID's of files to remove
   */
  def remove(
      uid: UserId,
      ids: Seq[UUID]
  )(implicit ec: ExecutionContext): Future[Unit]
}

trait PasswordAuthRepository extends DelegableAuthInfoDAO[PasswordInfo]

trait OAuth2Repository extends DelegableAuthInfoDAO[OAuth2Info]

//trait OpenIDAuthRepository extends DelegableAuthInfoDAO[OpenIDInfo]
