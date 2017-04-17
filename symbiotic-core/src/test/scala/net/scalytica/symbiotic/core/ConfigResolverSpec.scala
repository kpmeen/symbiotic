package net.scalytica.symbiotic.core

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

class ConfigResolverSpec extends Specification {

  val repoProviderName = "net.scalytica.symbiotic.test.TestRepositoryProvider$"

  val confMap = {
    import scala.collection.JavaConverters._
    Map("symbiotic.repository" -> repoProviderName).asJava
  }
  val config = ConfigFactory.parseMap(confMap)

  val resolver = new ConfigResolver(config)

  val bogusConf = {
    import scala.collection.JavaConverters._
    Map("symbiotic.repository" -> "foo.bar.Baz$").asJava
  }
  val bogusConfig = ConfigFactory.parseMap(bogusConf)

  "ConfigResolver" should {
    "correctly resolve the RepositoryProvider" in {
      resolver.repoInstance.getClass.getName must_== repoProviderName
    }

    "throw a ClassNotFoundException when RepositoryProvider isn't found" in {
      val bogusResolver = new ConfigResolver(bogusConfig)
      bogusResolver.repoInstance must throwA[ClassNotFoundException]
    }
  }

}
