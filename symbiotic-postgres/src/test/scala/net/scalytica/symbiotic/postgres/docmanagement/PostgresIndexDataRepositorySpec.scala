package net.scalytica.symbiotic.postgres.docmanagement

import net.scalytica.symbiotic.fs.FileSystemIO
import net.scalytica.symbiotic.test.specs.{
  IndexDataRepositorySpec,
  PostgresSpec
}

class PostgresIndexDataRepositorySpec
    extends IndexDataRepositorySpec
    with PostgresSpec {

  val fs                     = new FileSystemIO(config)
  override val folderRepo    = new PostgresFolderRepository(config)
  override val fileRepo      = new PostgresFileRepository(config, fs)
  override val indexProvider = new PostgresIndexDataRepository(config, fs)

}
