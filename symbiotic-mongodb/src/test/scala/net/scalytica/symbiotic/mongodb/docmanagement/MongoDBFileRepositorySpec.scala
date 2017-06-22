package net.scalytica.symbiotic.mongodb.docmanagement

import net.scalytica.symbiotic.test.specs.{FileRepositorySpec, MongoSpec}

class MongoDBFileRepositorySpec extends FileRepositorySpec with MongoSpec {

  override val folderRepo = new MongoDBFolderRepository(config)
  override val fileRepo   = new MongoDBFileRepository(config)

}
