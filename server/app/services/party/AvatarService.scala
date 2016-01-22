/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package services.party

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.party.Avatar
import models.party.PartyBaseTypes.UserId
import repository.AvatarRepository

@Singleton
class AvatarService @Inject() (val avatarRepository: AvatarRepository) {

  def save(a: Avatar): Option[UUID] = avatarRepository.save(a)

  def get(uid: UserId): Option[Avatar] = avatarRepository.get(uid)

  def remove(uid: UserId): Unit = avatarRepository.remove(uid)

  def remove(uid: UserId, ids: Seq[UUID]): Unit = avatarRepository.remove(uid, ids)

}
