package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.repository.RepositoryProvider

object TestRepositoryProvider extends RepositoryProvider {
  override lazy val fileRepository      = new TestFileRepository()
  override lazy val folderRepository    = new TestFolderRepository()
  override lazy val fsTreeRepository    = new TestFSTreeRepository()
  override lazy val indexDataRepository = new TestIndexDataRepository()
}
