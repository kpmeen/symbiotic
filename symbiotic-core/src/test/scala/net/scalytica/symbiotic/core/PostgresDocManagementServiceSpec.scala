package net.scalytica.symbiotic.core

import net.scalytica.symbiotic.test.specs.PostgresSpec

class PostgresDocManagementServiceSpec
    extends DocManagementServiceSpec
    with PostgresSpec {

  // See DocManagementServiceSpec

  val service = new DocManagementService(new ConfigResolver(config))
}
