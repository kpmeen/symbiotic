package net.scalytica.symbiotic.core

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.scalytica.symbiotic.api.persistence.RepositoryProvider

object ConfigResolver {

  lazy val Config       = ConfigFactory.load()
  lazy val RepoInstance = resolveRepoInstance(Config)

  private val SingletonModule = "MODULE$"

  private[core] def resolveRepoInstance(conf: Config): RepositoryProvider = {
    val repoObjStr = conf.as[String]("symbiotic.repository")

    Class
      .forName(repoObjStr)
      .getField(SingletonModule)
      .get(null) // scalastyle:ignore
      .asInstanceOf[RepositoryProvider]
  }

}
