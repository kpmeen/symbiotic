package net.scalytica.symbiotic.mongodb.docmanagement

import net.scalytica.symbiotic.test.specs.{FSTreeRepositorySpec, MongoSpec}

class MongoDBFSTreeRepositorySpec extends FSTreeRepositorySpec with MongoSpec {

  override val folderRepo = new MongoDBFolderRepository(config)
  override val fstreeRepo = new MongoDBFSTreeRepository(config)

}
