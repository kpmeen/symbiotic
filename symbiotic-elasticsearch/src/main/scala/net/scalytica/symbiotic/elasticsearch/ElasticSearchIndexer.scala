package net.scalytica.symbiotic.elasticsearch

import java.util.Base64

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.BulkResponseItem
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.sksamuel.elastic4s.streams.{RequestBuilder, ResponseListener}
import com.sksamuel.elastic4s.{IndexAndType, RefreshPolicy}
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.indexing.Indexer
import net.scalytica.symbiotic.api.types.{FileStream, ManagedFile}
import net.scalytica.symbiotic.json.Implicits.ManagedFileFormat
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Indexer implementation for integrating with ElasticSearch.
 */
class ElasticSearchIndexer private[elasticsearch] (
    cfg: ElasticSearchConfig,
    refreshPolicy: RefreshPolicy = RefreshPolicy.NONE
)(implicit sys: ActorSystem, mat: Materializer, ec: ExecutionContext)
    extends Indexer {

  private val logger = LoggerFactory.getLogger(getClass)

  private[elasticsearch] lazy val esClient = new ElasticSearchClient(cfg)

  /** Defines if ES should refresh after each record when streaming */
  private[this] val refreshEach = RefreshPolicy.IMMEDIATE == refreshPolicy

  /** Index definition for ManageFile metadata */
  private[this] def managedFileIndexDef(mf: ManagedFile): IndexDefinition = {
    indexInto(cfg.metadataIdxAndType).doc(mf)
  }

  private[this] def fileContentIndexDef(base64Str: String): IndexDefinition = {
    indexInto(cfg.filesIdxAndType)
      .doc(Json.stringify(Json.obj("data" -> base64Str)))
      .pipeline(ElasticSearchClient.attachmentPipelineId)
  }

  private[this] implicit val mngdFileReqBuilder: RequestBuilder[ManagedFile] = {
    t: ManagedFile =>
      // TODO: Set stream to None before sending to ES.
      indexInto(cfg.metadataIdxAndType).doc[ManagedFile](t)
  }

  /** Send ManagedFile metadata to ES for indexing */
  private[this] def indexMetadata(a: ManagedFile): Future[Boolean] = {
    esClient.exec(managedFileIndexDef(a).refresh(refreshPolicy)).map {
      case Right(r) =>
        logger.debug(
          s"ManagedFile was${if (r.isError) " not" else ""} indexed"
        )
        r.isSuccess

      case Left(err) =>
        logger.warn(s"An error occurred indexing a file: ${err.error}")
        err.isError
    }
  }

  /** Helper to encode a byte array into a base64 encoded String */
  private[this] def encodeFile(data: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(data)
  }

  /** Helper to convert an akka-streams Source to an Array[Byte] */
  private[this] def fileSourceToBytes(fs: FileStream): Future[Array[Byte]] = {
    fs.runFold(Array.empty[Byte])((s, c) => s ++ c)
  }

  /** Send stream/file content to ES for indexing */
  private[this] def indexFile(a: ManagedFile): Future[Boolean] = {
    a.fileType.flatMap { ft =>
      if (cfg.indexable(ft)) {
        a.stream.map { fs =>
          fileSourceToBytes(fs).map(encodeFile).flatMap { base64Str =>
            esClient
              .exec(fileContentIndexDef(base64Str).refresh(refreshPolicy))
              .map {
                case Right(r) =>
                  logger.debug(
                    s"File was${if (r.isError) " not" else ""} indexed"
                  )
                  r.isSuccess

                case Left(err) =>
                  logger.warn(s"Error when indexing a file: ${err.error}")
                  err.isError
              }
          }
        }
      } else None
    }.getOrElse(Future.successful(true))
  }

  override def index(
      a: ManagedFile
  )(implicit ec: ExecutionContext): Future[Boolean] =
    for {
      md <- indexMetadata(a)
      fi <- indexFile(a)
    } yield md && fi

  private[this] implicit val mngdFileListener: ResponseListener[ManagedFile] = {
    (resp: BulkResponseItem, original: ManagedFile) =>
      if (logger.isDebugEnabled)
        logger.debug(
          s"Indexing file ${original.filename} returned ${resp.result}"
        )
  }

  /** Helper function to build a Sink from an elastic4s subscriber */
  private[this] def elasticSink[T](completionMsg: String)(
      implicit rb: RequestBuilder[T],
      listener: ResponseListener[T]
  ): Sink[T, NotUsed] = {
    Sink.fromSubscriber[T](
      esClient.httpClient.subscriber[T](
        refreshAfterOp = refreshEach,
        completionFn = () => logger.debug(completionMsg),
        listener = listener
      )(rb, sys)
    )
  }

  override def indexSource(
      src: Source[ManagedFile, NotUsed],
      includeFiles: Boolean = false
  )(implicit ec: ExecutionContext): Unit = {
    val fileToByteArray =
      Flow[ManagedFile].filter(_.metadata.isFile).mapAsync(1)(indexFile)

    val metadataSink    = elasticSink[ManagedFile]("ManagedFile was indexed")
    val fileContentSink = fileToByteArray to Sink.ignore

    src runWith Flow[ManagedFile].alsoTo(fileContentSink).to(metadataSink)
  }

}

object ElasticSearchIndexer {

  def apply(config: Config)(
      implicit sys: ActorSystem,
      mat: Materializer,
      ec: ExecutionContext
  ): ElasticSearchIndexer = {
    initIndices(config)
    new ElasticSearchIndexer(config)
  }

  def apply(config: Config, refreshPolicy: RefreshPolicy)(
      implicit sys: ActorSystem,
      mat: Materializer,
      ec: ExecutionContext
  ): ElasticSearchIndexer = {
    initIndices(config)
    new ElasticSearchIndexer(config, refreshPolicy)
  }

  /** Initialize the indices when the class is instantiated. */
  private[elasticsearch] def initIndices(cfg: ElasticSearchConfig): Boolean = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val c = new ElasticSearchClient(cfg)

    def createIfNotExists(it: IndexAndType): Future[Boolean] = {
      c.exec(indexExists(it.index)).flatMap {
        case Right(er) =>
          if (!er.result.exists) {
            // TODO: Add specific mappings for ManagedFile types
            c.exec(createIndex(it.index) mappings mapping(it.`type`)).map {
              case Right(cir) => cir.result.acknowledged
              case Left(_)    => false
            }
          } else {
            Future.successful(false)
          }

        case Left(_) =>
          Future.successful(false)
      }
    }

    def enableIngest(): Future[Boolean] = {
      // If fileIndexingEnabled is set to true, we need to check if the ingest
      // attachment pipeline is initialised. If not, we set it up.
      if (cfg.fileIndexingEnabled) {
        c.attachmentPipelineExists.flatMap { pipelineExists =>
          if (pipelineExists) Future.successful(true)
          else c.initAttachmentPipeline()
        }
      } else Future.successful(true)
    }

    val res = Await.result(for {
      a <- createIfNotExists(cfg.metadataIdxAndType)
      b <- createIfNotExists(cfg.filesIdxAndType)
      c <- enableIngest()
    } yield a && b && c, 10 seconds)

    c.close()
    res
  }

  /** Removes the indices entirely from ElasticSearch */
  private[elasticsearch] def removeIndicies(
      cfg: ElasticSearchConfig
  ): Boolean = {
    import ExecutionContext.Implicits.global
    val c = new ElasticSearchClient(cfg)

    def deleteIfExists(it: IndexAndType): Future[Boolean] = {
      c.exec(indexExists(it.index)).flatMap {
        case Right(er) =>
          if (er.result.exists)
            c.exec(deleteIndex(it.index)).map {
              case Right(di) => di.result.acknowledged
              case Left(_)   => false
            } else Future.successful(false)

        case Left(_) =>
          Future.successful(false)
      }
    }

    val res =
      Await.result(for {
        a <- c.removeAttachmentPipeline()
        b <- deleteIfExists(cfg.filesIdxAndType)
        c <- deleteIfExists(cfg.metadataIdxAndType)
      } yield a && b && c, 10 seconds)

    c.close()
    res
  }

}
