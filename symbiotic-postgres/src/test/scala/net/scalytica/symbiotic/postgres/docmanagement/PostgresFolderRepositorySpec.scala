package net.scalytica.symbiotic.postgres.docmanagement

import net.scalytica.symbiotic.test.specs.{FolderRepositorySpec, PostgresSpec}

class PostgresFolderRepositorySpec
    extends FolderRepositorySpec
    with PostgresSpec {

  override val folderRepo = new PostgresFolderRepository(config)
}
