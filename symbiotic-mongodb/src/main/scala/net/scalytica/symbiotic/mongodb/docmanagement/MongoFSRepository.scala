package net.scalytica.symbiotic.mongodb.docmanagement

import net.scalytica.symbiotic.mongodb.{DManFS, WithMongoIndex}
import net.scalytica.symbiotic.data.MetadataKeys._
import com.typesafe.config.Config

trait MongoFSRepository extends DManFS

class ManagedFilesIndex(
    val configuration: Config
) extends MongoFSRepository
    with WithMongoIndex {

  ensureIndex()

  override def ensureIndex(): Unit = {
    val indexKeys = List(
      Indexable("filename"),
      Indexable(OwnerKey.full),
      Indexable(FidKey.full),
      Indexable(UploadedByKey.full),
      Indexable(PathKey.full),
      Indexable(VersionKey.full),
      Indexable(IsFolderKey.full)
    )
    index(indexKeys, collection)
  }
}
