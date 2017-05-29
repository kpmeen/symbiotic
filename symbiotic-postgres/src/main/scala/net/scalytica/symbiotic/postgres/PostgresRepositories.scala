package net.scalytica.symbiotic.postgres

import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.api.persistence.RepositoryProvider
import net.scalytica.symbiotic.postgres.docmanagement.{
  PostgresFSTreeRepository,
  PostgresFileRepository,
  PostgresFolderRepository
}

class PostgresRepositories extends RepositoryProvider {

  lazy val config = ConfigFactory.load()

  override lazy val fileRepository = new PostgresFileRepository(config)

  override lazy val folderRepository = new PostgresFolderRepository(config)

  override lazy val fsTreeRepository = new PostgresFSTreeRepository(config)
}
