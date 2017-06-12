package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.persistence.FolderRepository
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{Folder, FolderId, Path, TransUserId}

import scala.concurrent.ExecutionContext

class TestFolderRepository extends FolderRepository {
  override def save(
      f: Folder
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def get(
      folderId: FolderId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def exists(
      at: Path
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def filterMissing(
      p: Path
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def move(orig: Path, mod: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???
}
