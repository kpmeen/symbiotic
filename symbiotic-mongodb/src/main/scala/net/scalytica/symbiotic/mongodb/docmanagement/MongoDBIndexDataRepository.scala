package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{Attributes, Materializer}
import akka.stream.scaladsl.Source
import com.mongodb.Bytes
import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.IndexDataRepository
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class MongoDBIndexDataRepository(
    val configuration: Config
) extends MongoFSRepository
    with IndexDataRepository {

  private val log = LoggerFactory.getLogger(this.getClass)

  log.debug(
    s"Using configuration" + configuration.getConfig(
      "symbiotic.persistence.mongodb"
    )
  )

  private[this] def getGfsFile(id: UUID)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = Future {
    gfs
      .findOne(
        MongoDBObject(
          "_id"            -> id.toString,
          IsFolderKey.full -> false
        )
      )
      .map(file_fromGridFS)
  }

  def streamFiles()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[File, NotUsed] = {
    implicit val ec = as.dispatcher
    val cursor = collection
      .find(
        $and(
          IsFolderKey.full $eq false,
          IsDeletedKey.full $eq false
        )
      )
      .sort(MongoDBObject(FidKey.full -> -1, VersionKey.full -> -1))

    cursor.options_=(Bytes.QUERYOPTION_NOTIMEOUT)

    // Set up the stream, Ordererd by FileId and then by Version.
    Source
      .fromIterator(() => cursor)
      .addAttributes(Attributes.asyncBoundary)
      .map(file_fromBSON)
      // Prepend an empty value so not to lose the first element in mapConcat
      .prepend(Source.single(File.empty))
      // slide over 2 elems at a time for comparison in mapConcat
      .sliding(2, 1)
      // Deduplicate by only keeping the first element of a given FileId, which
      // is also the latest version according to the sorting.
      .mapConcat {
        case prev +: curr +: _ =>
          if (prev.metadata.fid == curr.metadata.fid) Nil
          else List(curr)
      }
      .mapAsync(1)(f => getGfsFile(f.id.get).map(_.getOrElse(f)))
  }

  def streamFolders()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[Folder, NotUsed] = {
    val cursor = collection.find(
      MongoDBObject(
        IsFolderKey.full  -> true,
        IsDeletedKey.full -> false
      )
    )
    cursor.options_=(Bytes.QUERYOPTION_NOTIMEOUT)

    Source
      .fromIterator(() => cursor)
      .addAttributes(Attributes.asyncBoundary)
      .map(folder_fromBSON)
  }

}
