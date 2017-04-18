package services.party

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import repository.mongodb.AvatarRepository

@Singleton
class AvatarService @Inject()(val repository: AvatarRepository) {

  def save(a: Avatar): Option[UUID] = repository.save(a)

  def get(uid: UserId): Option[Avatar] = repository.get(uid)

  def remove(uid: UserId): Unit = repository.remove(uid)

  def remove(uid: UserId, ids: Seq[UUID]): Unit = repository.remove(uid, ids)

}
