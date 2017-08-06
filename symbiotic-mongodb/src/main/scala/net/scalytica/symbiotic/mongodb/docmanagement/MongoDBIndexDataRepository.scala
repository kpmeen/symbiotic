package net.scalytica.symbiotic.mongodb.docmanagement

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{Attributes, Materializer}
import akka.stream.scaladsl.Source
import com.mongodb.Bytes
import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.IndexDataRepository
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.{File, Folder, SymbioticContext}
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.slf4j.LoggerFactory

class MongoDBIndexDataRepository(
    val configuration: Config
) extends MongoFSRepository
    with IndexDataRepository {

  private val logger = LoggerFactory.getLogger(this.getClass)

  logger.debug(
    s"Using configuration ${configuration.getConfig("symbiotic.mongodb")}"
  )

  def streamFiles()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[File, NotUsed] = {
    val cursor = collection.find(MongoDBObject(IsFolderKey.full -> false))
    cursor.options_=(Bytes.QUERYOPTION_NOTIMEOUT)

    Source
      .fromIterator(() => cursor)
      .addAttributes(Attributes.asyncBoundary)
      .map(file_fromBSON)
  }

  def streamFolders()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[Folder, NotUsed] = {
    val cursor = collection.find(MongoDBObject(IsFolderKey.full -> true))
    cursor.options_=(Bytes.QUERYOPTION_NOTIMEOUT)

    Source
      .fromIterator(() => cursor)
      .addAttributes(Attributes.asyncBoundary)
      .map(folder_fromBSON)
  }

}
