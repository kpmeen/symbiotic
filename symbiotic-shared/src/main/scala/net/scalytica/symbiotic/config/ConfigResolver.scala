package net.scalytica.symbiotic.config

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.RepositoryProvider
import org.slf4j.LoggerFactory

final class ConfigResolver(config: Config = ConfigReader.load()) {

  private[this] val log = LoggerFactory.getLogger(getClass)

  lazy val repoInstance: RepositoryProvider = resolveRepoInstance()

  private[this] def resolveRepoInstance(): RepositoryProvider = {
    val repoObjStr = config.getString("symbiotic.repository")

    log.info(s"Using repository $repoObjStr as backend for persistence.")

    Class
      .forName(repoObjStr)
      .getField(ConfigResolver.SingletonModule)
      .get(null) // scalastyle:ignore
      .asInstanceOf[RepositoryProvider]
  }

}

private[config] object ConfigResolver {

  val SingletonModule = "MODULE$"

}
