package net.scalytica.symbiotic.postgres

import net.scalytica.symbiotic.api.repository.RepositoryProvider
import net.scalytica.symbiotic.config.ConfigReader
import net.scalytica.symbiotic.fs.FileSystemIO
import net.scalytica.symbiotic.postgres.docmanagement.{
  PostgresFSTreeRepository,
  PostgresFileRepository,
  PostgresFolderRepository,
  PostgresIndexDataRepository
}

object PostgresRepositories extends RepositoryProvider {

  lazy val config = ConfigReader.load()

  lazy val fileSystemIO = new FileSystemIO(config)

  override lazy val fileRepository =
    new PostgresFileRepository(config, fileSystemIO)

  override lazy val folderRepository = new PostgresFolderRepository(config)

  override lazy val fsTreeRepository = new PostgresFSTreeRepository(config)

  override lazy val indexDataRepository =
    new PostgresIndexDataRepository(config, fileSystemIO)
}
