package net.scalytica.symbiotic.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.BulkResponseItem
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.sksamuel.elastic4s.streams.{RequestBuilder, ResponseListener}
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.indexing.Indexer
import net.scalytica.symbiotic.api.types.ManagedFile
import net.scalytica.symbiotic.json.Implicits.ManagedFileFormat
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Indexer implementation for integrating with ElasticSearch.
 */
class ElasticSearchIndexer private[elasticsearch] (
    cfg: ElasticSearchConfig,
    refreshPolicy: RefreshPolicy = RefreshPolicy.NONE
)(implicit sys: ActorSystem, mat: Materializer)
    extends Indexer {

  private val logger = LoggerFactory.getLogger(getClass)

  implicit val reqBuilder = new RequestBuilder[ManagedFile] {
    def request(t: ManagedFile): BulkCompatibleDefinition = {
      indexInto(cfg.indexAndType).doc[ManagedFile](t)
    }
  }

  private[this] lazy val escClient = new ElasticSearchClient(cfg)

  override def index(
      a: ManagedFile
  )(implicit ec: ExecutionContext): Future[Boolean] = {
    escClient
      .exec(
        indexInto(cfg.indexAndType).doc(a).refresh(refreshPolicy)
      )
      .map { r =>
        logger.debug(
          s"ManagedFile was${if (!r.created) " not" else ""} indexed"
        )
        r.created
      }
  }

  override def indexSource(
      src: Source[ManagedFile, NotUsed],
      includeFiles: Boolean = false
  )(implicit ec: ExecutionContext): Unit = {
    val refreshEach = RefreshPolicy.IMMEDIATE == refreshPolicy

    // TODO: if includeFiles == true the stream should also fetch the actual
    // files (if any), and pass it to ES.
    // (requires the Ingest Attachments plugin)

    val sink = Sink.fromSubscriber(
      escClient.httpClient.subscriber[ManagedFile](
        refreshAfterOp = refreshEach,
        completionFn = () => logger.info(s"Completed indexing Source"),
        listener = new ResponseListener[ManagedFile] {
          override def onAck(resp: BulkResponseItem, original: ManagedFile) = {
            if (logger.isDebugEnabled)
              logger.debug(
                s"Indexing file ${original.filename} returned ${resp.result}"
              )
          }
        }
      )
    )

    src.runWith(sink)
  }

}

object ElasticSearchIndexer {

  private val logger = LoggerFactory.getLogger(classOf[ElasticSearchIndexer])

  def apply(config: Config)(
      implicit sys: ActorSystem,
      mat: Materializer
  ): ElasticSearchIndexer = {
    initIndices(config)
    new ElasticSearchIndexer(config)
  }

  def apply(config: Config, refreshPolicy: RefreshPolicy)(
      implicit sys: ActorSystem,
      mat: Materializer
  ): ElasticSearchIndexer = {
    initIndices(config)
    new ElasticSearchIndexer(config, refreshPolicy)
  }

  /** Initialize the indices when the class is instantiated. */
  private[elasticsearch] def initIndices(cfg: ElasticSearchConfig): Boolean = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val c = new ElasticSearchClient(cfg)
    val res =
      Await.result(
        c.exec(indexExists(cfg.indexName)).flatMap { er =>
          // TODO: Add specific mappings for ManagedFile types
          if (!er.exists)
            c.exec(
                createIndex(cfg.indexName) mappings mapping(cfg.indexType)
              )
              .map(_.acknowledged)
          else Future.successful(false)
        },
        10 seconds
      )

    c.close()
    res
  }

  /** Removes the indices entirely from ElasticSearch */
  private[elasticsearch] def removeIndicies(
      cfg: ElasticSearchConfig
  ): Boolean = {
    import ExecutionContext.Implicits.global
    val c = new ElasticSearchClient(cfg)
    val res =
      Await.result(
        c.exec(indexExists(cfg.indexName)).flatMap { er =>
          if (er.exists)
            c.exec(deleteIndex(cfg.indexName)).map(_.acknowledged)
          else Future.successful(false)
        },
        10 seconds
      )

    c.close()
    res
  }

}
