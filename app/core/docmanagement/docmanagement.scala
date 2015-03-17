/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core

package object docmanagement {

  // Defining some useful type aliases
  type Metadata = Map[String, Any]
  type FileStream = java.io.InputStream
  type Version = Int
  type FolderId = FileId

  /**
   * Key definitions for metadata content in the gridfs files table.
   */
  private[docmanagement] object MetadataKeys {
    val MetadataKey = "metadata"

    case class Key(key: String, parent: Option[Key] = None) {
      def full: String = parent.map(p => s"${p.full}.$key").getOrElse(s"$MetadataKey.$key")

      def partial: String = parent.map(p => s"${p.key}").getOrElse(key)
    }

    val CidKey = new Key("cid")
    val PidKey = new Key("pid")
    val PathKey = new Key("path")
    val DescriptionKey = new Key("description")
    val VersionKey = new Key("version")
    val UploadedByKey = new Key("uploadedBy")
    val LockKey = new Key("lock")
    val LockedByKey = new Key("by", Some(LockKey))
    val LockDateKey = new Key("date", Some(LockKey))
    val IsFolderKey = new Key("isFolder")
  }

}
