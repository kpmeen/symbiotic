package net.scalytica.symbiotic.mongodb.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.mongodb.{DManFS, WithMongoIndex}

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
      Indexable(CreatedByKey.full),
      Indexable(PathKey.full),
      Indexable(VersionKey.full),
      Indexable(IsFolderKey.full)
    )
    index(indexKeys, collection)
  }
}
