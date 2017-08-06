package net.scalytica.symbiotic.core

import net.scalytica.symbiotic.test.specs.PostgresSpec

class PostgresDocManagementServiceSpec
    extends DocManagementServiceSpec
    with PostgresSpec {

  override val cfgResolver = new ConfigResolver(config)

}
