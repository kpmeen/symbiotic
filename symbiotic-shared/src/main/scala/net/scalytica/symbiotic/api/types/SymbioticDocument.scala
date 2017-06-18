package net.scalytica.symbiotic.api.types

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.StreamConverters
import org.joda.time.DateTime

trait SymbioticDocument[A <: BaseMetadata] {
  val id: Option[UUID]
  val filename: String
  val contentType: Option[String]
  val uploadDate: Option[DateTime]
  // Using String for length to prevent data loss when using lesser protocols
  // like JSON/JS. Where the precision is not all that good.
  val length: Option[String]
  val stream: Option[FileStream]
  val metadata: A

  def inputStream(
      implicit as: ActorSystem,
      mat: Materializer
  ): Option[java.io.InputStream] =
    stream.map(_.runWith(StreamConverters.asInputStream()))
}
