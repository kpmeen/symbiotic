/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.iteratee.Enumerator

import scala.concurrent.ExecutionContext

trait GridFSDocument[A <: BaseMetadata] {
  val id: Option[UUID]
  val filename: String
  val contentType: Option[String]
  val uploadDate: Option[DateTime]
  // using String for length to prevent data loss in JS clients
  val length: Option[String]
  val stream: Option[java.io.InputStream]
  val metadata: A

  /**
   * Feeds the InputStream bytes into an Enumerator
   */
  def enumerate(implicit ec: ExecutionContext): Option[Enumerator[Array[Byte]]] =
    stream.map(s => Enumerator.fromStream(s))
}