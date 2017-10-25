package net.scalytica.symbiotic.mongodb.docmanagement

import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.mongodb.{DManFS, WithMongoIndex}

trait MongoFSRepository extends DManFS with WithMongoIndex {

  override def ensureIndex(): Unit = {
    index(ManagedFilesIndexKeys.indexKeys, collection)
  }

  ensureIndex()

}

case class Indexable(keys: Seq[String], unique: Boolean = false)

object Indexable {
  def apply(key: String): Indexable = Indexable(Seq(key))

  def apply(key: String, unique: Boolean): Indexable =
    Indexable(Seq(key), unique)
}

object ManagedFilesIndexKeys {

  val indexKeys = List(
    Indexable("filename"),
    Indexable(PathKey.full),
    Indexable(OwnerKey.full),
    Indexable(FidKey.full),
    Indexable(CreatedByKey.full),
    Indexable(VersionKey.full),
    Indexable(
      Seq(
        "filename",
        PathKey.full,
        IsFolderKey.full,
        OwnerIdKey.full,
        VersionKey.full
      ),
      unique = true
    )
  )
}
