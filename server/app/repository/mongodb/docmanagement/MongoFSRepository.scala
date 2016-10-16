/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.docmanagement

import com.google.inject.{Inject, Singleton}
import models.docmanagement.MetadataKeys._
import play.api.Configuration
import repository.mongodb.{DManFS, WithMongoIndex}

trait MongoFSRepository extends DManFS

@Singleton
class ManagedFilesIndex @Inject() (
    val configuration: Configuration
) extends MongoFSRepository with WithMongoIndex {

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