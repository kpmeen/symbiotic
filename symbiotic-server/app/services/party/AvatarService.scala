package services.party

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import repository.mongodb.AvatarRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AvatarService @Inject()(repository: AvatarRepository) {

  def save(a: Avatar)(implicit ec: ExecutionContext): Future[Option[UUID]] =
    repository.save(a)

  def get(uid: UserId)(implicit ec: ExecutionContext): Future[Option[Avatar]] =
    repository.get(uid)

  def remove(uid: UserId)(implicit ec: ExecutionContext): Future[Unit] =
    repository.remove(uid)

  def remove(
      uid: UserId,
      ids: Seq[UUID]
  )(implicit ec: ExecutionContext): Future[Unit] = repository.remove(uid, ids)

}
