/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import core.mongodb.{DManFS, WithMongoIndex}
import dman.MetadataKeys._
import org.joda.time.DateTime

trait BaseFile {
  val id: Option[FileId]
  val filename: String
  val contentType: Option[String]
  val uploadDate: Option[DateTime]
  val metadata: FileMetadata
}

object BaseFile extends DManFS with WithMongoIndex {

  override def ensureIndex(): Unit = {
    val indexKeys = List(
      Indexable("filename"),
      Indexable(CidKey.full),
      Indexable(UploadedByKey.full),
      Indexable(PathKey.full),
      Indexable(VersionKey.full),
      Indexable(IsFolderKey.full)
    )
    index(indexKeys, collection)
  }

}
