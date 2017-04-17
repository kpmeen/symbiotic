package net.scalytica.symbiotic.core

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.scalytica.symbiotic.api.persistence.RepositoryProvider

class ConfigResolver(config: Config = ConfigFactory.load()) {

  lazy val repoInstance: RepositoryProvider = resolveRepoInstance(config)

  private def resolveRepoInstance(conf: Config): RepositoryProvider = {
    val repoObjStr = conf.as[String]("symbiotic.repository")

    Class
      .forName(repoObjStr)
      .getField(ConfigResolver.SingletonModule)
      .get(null) // scalastyle:ignore
      .asInstanceOf[RepositoryProvider]
  }

}

private[core] object ConfigResolver {
  val SingletonModule = "MODULE$"
}
