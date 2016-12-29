/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import java.util.UUID

import akka.stream.IOResult
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import org.joda.time.DateTime
import play.api.libs.iteratee.Enumerator

import scala.concurrent.{ExecutionContext, Future}

trait GridFSDocument[A <: BaseMetadata] {
  val id: Option[UUID]
  val filename: String
  val contentType: Option[String]
  val uploadDate: Option[DateTime]
  // Using String for length to prevent data loss in JS clients
  val length: Option[String]
  val stream: Option[java.io.InputStream]
  val metadata: A

  /**
   * Feeds the InputStream bytes into an Enumerator
   */
  def enumerate(implicit ec: ExecutionContext): Option[Enumerator[Array[Byte]]] =
    stream.map(s => Enumerator.fromStream(s))

  def source: Option[Source[ByteString, Future[IOResult]]] =
    stream.map(s => StreamConverters.fromInputStream(() => s))

}