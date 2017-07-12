package services.party

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import repository.mongodb.AvatarRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AvatarService @Inject()(
    implicit ec: ExecutionContext,
    repository: AvatarRepository
) {

  def save(a: Avatar): Future[Option[UUID]] = repository.save(a)

  def get(uid: UserId): Future[Option[Avatar]] = repository.get(uid)

  def remove(uid: UserId): Future[Unit] = repository.remove(uid)

  def remove(uid: UserId, ids: Seq[UUID]): Future[Unit] =
    repository.remove(uid, ids)

}
