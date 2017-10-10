package net.scalytica.symbiotic.core

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.scalytica.symbiotic.api.repository.RepositoryProvider
import org.slf4j.LoggerFactory

final class ConfigResolver(config: Config = ConfigFactory.load()) {

  private val log = LoggerFactory.getLogger(getClass)

  lazy val repoInstance: RepositoryProvider = resolveRepoInstance()

  private def resolveRepoInstance(): RepositoryProvider = {
    val repoObjStr = config.as[String]("symbiotic.repository")

    log.info(s"Using repository $repoObjStr as backend for persistence.")

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
