package net.scalytica.symbiotic.elasticsearch

import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

case class ElasticSearchConfig(
    indexName: String,
    host: String,
    port: Int,
    protocol: String,
    fileIndexingEnabled: Boolean,
    fileTypes: Seq[String] = Seq.empty
) {

  val metadataIdxType                  = "metadata"
  val metadataIdxName                  = s"$indexName-$metadataIdxType"
  val metadataIdxAndType: IndexAndType = metadataIdxName / metadataIdxType

  val filesIdxType                  = "files"
  val filesIdxName                  = s"$indexName-$filesIdxType"
  val filesIdxAndType: IndexAndType = filesIdxName / filesIdxType

  def indexable(fileType: String): Boolean = fileTypes.contains(fileType)
}

object ElasticSearchConfig {

  implicit def fromConfig(config: Config): ElasticSearchConfig = {
    config.as[ElasticSearchConfig]("symbiotic.index.elasticsearch")
  }

}
