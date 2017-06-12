package net.scalytica.symbiotic.mongodb.docmanagement

import net.scalytica.symbiotic.test.specs.{FolderRepositorySpec, MongoSpec}

class MongoDBFolderRepositorySpec extends FolderRepositorySpec with MongoSpec {

  override val folderRepo = new MongoDBFolderRepository(config)

}
