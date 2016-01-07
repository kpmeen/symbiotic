/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.google.inject.{Inject, Singleton}
import models.party.Avatar
import models.party.PartyBaseTypes.UserId
import org.bson.types.ObjectId
import repository.AvatarRepository

@Singleton
class AvatarService @Inject() (val avatarRepository: AvatarRepository[ObjectId]) {

  def save(a: Avatar): Option[ObjectId] = avatarRepository.save(a)

  def get(uid: UserId): Option[Avatar] = avatarRepository.get(uid)

  def remove(uid: UserId): Unit = avatarRepository.remove(uid)

  def remove(uid: UserId, ids: Seq[ObjectId]): Unit = avatarRepository.remove(uid, ids)

}
