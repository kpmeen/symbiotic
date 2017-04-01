package repository.mongodb

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.base.Username
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import models.party.{Avatar, User}
import net.scalytica.symbiotic.core.SuccessOrFailure

trait UserRepository {

  def save(user: User): SuccessOrFailure

  def findById(id: UserId): Option[User]

  def findByUsername(username: Username): Option[User]

  def findByLoginInfo(loginInfo: LoginInfo): Option[User]

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
  def save(a: Avatar): Option[UUID]

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param uid UserId
   * @return Option[File]
   */
  def get(uid: UserId): Option[Avatar]

  /**
   * Removes _all_ avatar images where filename equals the uid
   *
   * @param uid UserId to remove avatar images for
   */
  def remove(uid: UserId): Unit

  /**
   *
   * @param uid UserId to remove files for.
   * @param ids a collection of the UUID's of files to remove
   */
  def remove(uid: UserId, ids: Seq[UUID]): Unit
}

trait PasswordAuthRepository extends DelegableAuthInfoDAO[PasswordInfo]

trait OAuth2Repository extends DelegableAuthInfoDAO[OAuth2Info]

//trait OpenIDAuthRepository extends DelegableAuthInfoDAO[OpenIDInfo]
