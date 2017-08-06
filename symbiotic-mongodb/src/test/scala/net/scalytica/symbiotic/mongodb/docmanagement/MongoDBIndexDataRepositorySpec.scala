package net.scalytica.symbiotic.mongodb.docmanagement

import net.scalytica.symbiotic.test.specs.{IndexDataRepositorySpec, MongoSpec}

class MongoDBIndexDataRepositorySpec
    extends IndexDataRepositorySpec
    with MongoSpec {

  override val folderRepo    = new MongoDBFolderRepository(config)
  override val fileRepo      = new MongoDBFileRepository(config)
  override val indexProvider = new MongoDBIndexDataRepository(config)

}
