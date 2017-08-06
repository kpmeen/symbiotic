package net.scalytica.symbiotic.elasticsearch

import net.scalytica.symbiotic.fs.FileSystemIO
import net.scalytica.symbiotic.postgres.docmanagement.{
  PostgresFileRepository,
  PostgresFolderRepository,
  PostgresIndexDataRepository
}
import net.scalytica.symbiotic.test.specs.PostgresSpec

class PostgresElasticSearchIndexerSpec
    extends ElasticSearchIndexerSpec
    with PostgresSpec {

  val fs                     = new FileSystemIO(config)
  override val fileRepo      = new PostgresFileRepository(config, fs)
  override val folderRepo    = new PostgresFolderRepository(config)
  override val indexDataRepo = new PostgresIndexDataRepository(config, fs)

}
