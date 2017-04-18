package net.scalytica.symbiotic.play

import net.scalytica.symbiotic.core.DocManagementService
import play.api.inject._
import play.api.{Configuration, Environment}

class SymbioticModule extends Module {

  def bindings(
      env: Environment,
      conf: Configuration
  ): Seq[Binding[DocManagementService]] = {
    Seq(bind[DocManagementService].toInstance(new DocManagementService()))
  }

}
