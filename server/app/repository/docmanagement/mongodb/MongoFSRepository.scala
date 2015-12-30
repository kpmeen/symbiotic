/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.docmanagement.mongodb

import core.mongodb.{DManFS, WithMongoIndex}
import models.docmanagement.MetadataKeys._

trait MongoFSRepository extends DManFS

object ManagedFilesIndex extends MongoFSRepository with WithMongoIndex {

  ensureIndex()

  override def ensureIndex(): Unit = {
    val indexKeys = List(
      Indexable("filename"),
      Indexable(OidKey.full),
      Indexable(FidKey.full),
      Indexable(UploadedByKey.full),
      Indexable(PathKey.full),
      Indexable(VersionKey.full),
      Indexable(IsFolderKey.full)
    )
    index(indexKeys, collection)
  }
}