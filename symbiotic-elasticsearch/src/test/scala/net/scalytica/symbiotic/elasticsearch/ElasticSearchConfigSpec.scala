package net.scalytica.symbiotic.elasticsearch

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{MustMatchers, WordSpec}

class ElasticSearchConfigSpec extends WordSpec with MustMatchers {

  val esHost = sys.props
    .get("CI")
    .orElse(sys.env.get("CI"))
    .map(_ => "elasticsearch")
    .getOrElse("127.0.0.1")

  val cfg: Config = ConfigFactory.load()

  s"The ${classOf[ElasticSearchConfig].getSimpleName}" should {

    "parse the reference.conf file" in {

      val res = ElasticSearchConfig.fromConfig(cfg)

      res.indexName mustBe "symbiotic"
      res.host mustBe esHost
      res.port mustBe 9200
      res.protocol mustBe "http"
      res.fileIndexingEnabled mustBe true
      res.fileTypes must have size 135
    }

  }

}
