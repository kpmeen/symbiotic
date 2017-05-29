package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FolderRepository
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._

import scala.concurrent.ExecutionContext

class PostgresFolderRepository(val config: Config) extends FolderRepository {

  override def save(f: Folder)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def get(folderId: FolderId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def exists(at: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def filterMissing(p: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def move(orig: Path, mod: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

}
