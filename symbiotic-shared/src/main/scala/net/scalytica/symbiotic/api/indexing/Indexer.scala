package net.scalytica.symbiotic.api.indexing

import akka.NotUsed
import akka.stream.scaladsl.Source
import net.scalytica.symbiotic.api.types.ManagedFile
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 * Defines the contract that needs to be implemented for integrating a search
 * engine with Symbiotic data for indexing.
 */
trait Indexer {

  def index(mf: ManagedFile)(implicit ec: ExecutionContext): Future[Boolean]

  def indexSource(
      src: Source[ManagedFile, NotUsed],
      includeFiles: Boolean = false
  )(implicit ec: ExecutionContext): Unit

}

/**
 * No-op implementation of [[Indexer]]. Will be used by default if no other
 * implementation is provided.
 */
class NoOpIndexer extends Indexer {

  private[this] val logger = LoggerFactory.getLogger(classOf[NoOpIndexer])

  override def index(a: ManagedFile)(implicit ec: ExecutionContext) = {
    logger.debug(s"Element ${a.getClass} received for indexing will be ignored")
    Future.successful[Boolean](true)
  }

  override def indexSource(
      src: Source[ManagedFile, NotUsed],
      includeFiles: Boolean = false
  )(implicit ec: ExecutionContext): Unit = Future.successful[Unit] {
    logger.debug(s"Source received for indexing will be ignored")
  }

}
