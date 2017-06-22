package net.scalytica.symbiotic.postgres.docmanagement

import net.scalytica.symbiotic.fs.FileSystemIO
import net.scalytica.symbiotic.test.specs.{FileRepositorySpec, PostgresSpec}

class PostgresFileRepositorySpec extends FileRepositorySpec with PostgresSpec {

  val fs                  = new FileSystemIO(config)
  override val folderRepo = new PostgresFolderRepository(config)
  override val fileRepo   = new PostgresFileRepository(config, fs)

}
