package net.scalytica.symbiotic.play

import net.scalytica.symbiotic.config.ConfigResolver
import net.scalytica.symbiotic.core.DocManagementService
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class SymbioticModule extends Module {

  override def bindings(
      environment: Environment,
      configuration: Configuration
  ): Seq[Binding[DocManagementService]] = {
    val cfgr = new ConfigResolver(configuration.underlying)
    Seq(bind[DocManagementService].toInstance(new DocManagementService(cfgr)))
  }

}
