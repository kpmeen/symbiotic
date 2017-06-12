package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.persistence.RepositoryProvider

object TestRepositoryProvider extends RepositoryProvider {
  override lazy val fileRepository   = new TestFileRepository()
  override lazy val folderRepository = new TestFolderRepository()
  override lazy val fsTreeRepository = new TestFSTreeRepository()
}
