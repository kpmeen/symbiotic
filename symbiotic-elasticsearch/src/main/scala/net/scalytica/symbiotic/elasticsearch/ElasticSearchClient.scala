package net.scalytica.symbiotic.elasticsearch

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpEntity.StringEntity
import com.sksamuel.elastic4s.http.{
  HttpClient,
  HttpExecutable,
  RequestFailure,
  RequestSuccess
}

import scala.concurrent.{ExecutionContext, Future}

class ElasticSearchClient(cfg: ElasticSearchConfig) {

  val baseUrl = s"${cfg.protocol}://${cfg.host}:${cfg.port}"
  val attachmentPipelineUrl =
    s"$baseUrl/_ingest/pipeline/${ElasticSearchClient.attachmentPipelineId}"

  val esUri = ElasticsearchClientUri(
    host = cfg.host,
    port = cfg.port
  )
  val httpClient = HttpClient(esUri)

  def exec[T, U](r: T)(
      implicit exec: HttpExecutable[T, U],
      ec: ExecutionContext
  ): Future[Either[RequestFailure, RequestSuccess[U]]] =
    httpClient.execute(r)

  def close(): Unit = httpClient.close()

  def attachmentPipelineExists(
      implicit ec: ExecutionContext
  ): Future[Boolean] = {
    httpClient.client
      .async(
        method = "GET",
        endpoint = attachmentPipelineUrl,
        params = Map.empty[String, Any]
      )
      .map(_.statusCode == 200) // scalastyle:ignore
  }

  def initAttachmentPipeline()(
      implicit ec: ExecutionContext
  ): Future[Boolean] = {
    httpClient.client
      .async(
        method = "PUT",
        endpoint = attachmentPipelineUrl,
        params = Map.empty[String, Any],
        entity = StringEntity(
          content = ElasticSearchClient.attachmentConfig,
          contentType = Some("application/json")
        )
      )
      .map(_.statusCode == 200) // scalastyle:ignore
  }

  def removeAttachmentPipeline()(
      implicit ec: ExecutionContext
  ): Future[Boolean] = {
    httpClient.client
      .async(
        method = "DELETE",
        endpoint = attachmentPipelineUrl,
        params = Map.empty[String, Any]
      )
      .map(_.statusCode == 200) // scalastyle:ignore
  }

}

object ElasticSearchClient {
  val attachmentPipelineId = "symbiotic-ingest-attachment-pipeline"
  val attachmentConfig =
    """{
         "description" : "Extract attachment information",
         "processors" : [
           {
             "attachment" : {
               "field" : "data"
             }
           }
         ]
       }""".stripMargin
}
