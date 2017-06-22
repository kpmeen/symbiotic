package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.persistence.FileRepository
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{File, FileId, Path, TransUserId}

import scala.concurrent.ExecutionContext

class TestFileRepository extends FileRepository {
  override def save(
      f: File
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def findLatestByFileId(
      fid: FileId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def move(
      filename: String,
      orig: Path,
      mod: Path
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def find(
      filename: String,
      maybePath: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def findLatest(
      filename: String,
      maybePath: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def listFiles(
      path: Path
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def lock(
      fid: FileId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def unlock(
      fid: FileId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???
}
