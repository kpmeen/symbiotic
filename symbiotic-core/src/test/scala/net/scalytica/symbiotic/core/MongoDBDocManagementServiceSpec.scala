package net.scalytica.symbiotic.core

import net.scalytica.symbiotic.test.specs.MongoSpec

class MongoDBDocManagementServiceSpec
    extends DocManagementServiceSpec
    with MongoSpec {

  override val cfgResolver = new ConfigResolver(config)

}
