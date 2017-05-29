package net.scalytica.symbiotic.test

import java.util.UUID

import net.scalytica.symbiotic.api.persistence._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{File, FileId, Folder, Path, _}

import scala.concurrent.ExecutionContext

object TestRepositoryProvider extends RepositoryProvider {
  override lazy val fileRepository   = new TestFileRepository()
  override lazy val folderRepository = new TestFolderRepository()
  override lazy val fsTreeRepository = new TestFSTreeRepository()
}

class TestFileRepository extends FileRepository {
  override def save(
      f: File
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def get(
      id: UUID
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def getLatest(
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
      path: String
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def locked(
      fid: FileId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) =
    ???

  override def lock(
      fid: FileId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) =
    ???

  override def unlock(
      fid: FileId
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) =
    ???
}

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

class TestFSTreeRepository extends FSTreeRepository {
  override def treePaths(
      from: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def tree(
      from: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def children(
      from: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???
}
