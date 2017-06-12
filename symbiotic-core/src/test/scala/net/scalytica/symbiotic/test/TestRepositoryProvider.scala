package net.scalytica.symbiotic.test

import net.scalytica.symbiotic.api.persistence._
import net.scalytica.symbiotic.test.repositories.{
  TestFSTreeRepository,
  TestFileRepository,
  TestFolderRepository
}

object TestRepositoryProvider extends RepositoryProvider {
  override lazy val fileRepository   = new TestFileRepository()
  override lazy val folderRepository = new TestFolderRepository()
  override lazy val fsTreeRepository = new TestFSTreeRepository()
}
