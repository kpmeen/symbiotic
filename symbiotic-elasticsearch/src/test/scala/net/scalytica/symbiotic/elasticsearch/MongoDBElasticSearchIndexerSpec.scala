package net.scalytica.symbiotic.elasticsearch

import net.scalytica.symbiotic.mongodb.docmanagement.{
  MongoDBFileRepository,
  MongoDBFolderRepository,
  MongoDBIndexDataRepository
}
import net.scalytica.symbiotic.test.specs.MongoSpec

class MongoDBElasticSearchIndexerSpec
    extends ElasticSearchIndexerSpec
    with MongoSpec {

  override val folderRepo    = new MongoDBFolderRepository(config)
  override val fileRepo      = new MongoDBFileRepository(config)
  override val indexDataRepo = new MongoDBIndexDataRepository(config)
}
