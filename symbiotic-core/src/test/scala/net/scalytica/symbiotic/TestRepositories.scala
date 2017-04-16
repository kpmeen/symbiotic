package net.scalytica.symbiotic

import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data._
import net.scalytica.symbiotic.persistence.{
  FSTreeRepository,
  FileRepository,
  FolderRepository,
  RepositoryProvider
}

object TestRepositoryProvider extends RepositoryProvider {
  override lazy val fileRepository   = new TestFileRepository()
  override lazy val folderRepository = new TestFolderRepository()
  override lazy val fsTreeRepository = new TestFSTreeRepository()
}

class TestFileRepository extends FileRepository {
  override def save(f: File)(implicit uid: UserId) = ???

  override def get(id: FileId)(implicit uid: UserId) = ???

  override def getLatest(fid: FileId)(implicit uid: UserId) = ???

  override def move(
      filename: String,
      orig: Path,
      mod: Path
  )(implicit uid: UserId) = ???

  override def find(
      filename: String,
      maybePath: Option[Path]
  )(implicit uid: UserId) = ???

  override def findLatest(
      filename: String,
      maybePath: Option[Path]
  )(implicit uid: UserId) = ???

  override def listFiles(path: String)(implicit uid: UserId) = ???

  override def locked(fid: FileId)(implicit uid: UserId) = ???

  override def lock(fid: FileId)(implicit uid: UserId) = ???

  override def unlock(fid: FileId)(implicit uid: UserId) = ???
}

class TestFolderRepository extends FolderRepository {
  override def save(f: Folder)(implicit uid: UserId) = ???

  override def get(folderId: FolderId)(implicit uid: UserId) = ???

  override def exists(at: Path)(implicit uid: UserId) = ???

  override def filterMissing(p: Path)(implicit uid: UserId) = ???

  override def move(orig: Path, mod: Path)(implicit uid: UserId) = ???
}

class TestFSTreeRepository extends FSTreeRepository {
  override def treePaths(from: Option[Path])(implicit uid: UserId) = ???

  override def tree(from: Option[Path])(implicit uid: UserId) = ???

  override def children(from: Option[Path])(implicit uid: UserId) = ???
}
