package net.scalytica.symbiotic.elasticsearch

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.{
  HttpClient,
  HttpExecutable,
  RequestFailure,
  RequestSuccess
}

import scala.concurrent.Future

class ElasticSearchClient(cfg: ElasticSearchConfig) {

  val httpClient = HttpClient(
    ElasticsearchClientUri(
      host = cfg.host,
      port = cfg.port
    )
  )

  def exec[T, U](r: T)(
      implicit exec: HttpExecutable[T, U]
  ): Future[Either[RequestFailure, RequestSuccess[U]]] =
    httpClient.execute(r)

  def close(): Unit = httpClient.close()

}
