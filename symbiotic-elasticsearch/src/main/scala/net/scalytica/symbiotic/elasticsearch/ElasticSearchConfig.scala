package net.scalytica.symbiotic.elasticsearch

import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

case class ElasticSearchConfig(indexName: String, host: String, port: Int) {
  val indexType                  = "metadata"
  val indexAndType: IndexAndType = indexName / indexType
}

object ElasticSearchConfig {

  implicit def fromConfig(config: Config): ElasticSearchConfig = {
    config.as[ElasticSearchConfig]("symbiotic.index.elasticsearch")
  }

}
