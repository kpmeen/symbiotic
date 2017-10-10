package net.scalytica.symbiotic.play

import net.scalytica.symbiotic.core.{ConfigResolver, DocManagementService}
import play.api.inject.Module
import play.api.{Configuration, Environment}

class SymbioticModule extends Module {

  override def bindings(
      environment: Environment,
      configuration: Configuration
  ) = {
    val cfgr = new ConfigResolver(configuration.underlying)
    Seq(bind[DocManagementService].toInstance(new DocManagementService(cfgr)))
  }

}
