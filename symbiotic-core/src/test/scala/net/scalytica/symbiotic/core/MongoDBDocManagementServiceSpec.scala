package net.scalytica.symbiotic.core

import net.scalytica.symbiotic.test.specs.MongoSpec

class MongoDBDocManagementServiceSpec
    extends DocManagementServiceSpec
    with MongoSpec {

  // See DocManagementServiceSpec

  val service = new DocManagementService(new ConfigResolver(config))

}
