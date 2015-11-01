/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models

import com.mongodb.casbah.commons.MongoDBObject

package object docmanagement {

  // Defining some useful type aliases
  type Metadata = MongoDBObject
  type FileStream = java.io.InputStream
  type Version = Int
  type FolderId = FileId

  /**
   * Key definitions for metadata content in the gridfs files table.
   */
  object MetadataKeys {
    val MetadataKey = "metadata"

    case class Key(key: String, parent: Option[Key] = None) {
      def full: String = parent.map(p => s"${p.full}.$key").getOrElse(s"$MetadataKey.$key")

      def partial: String = parent.map(p => s"${p.key}").getOrElse(key)
    }

    val IdKey = new Key("id")
    val OidKey = new Key("oid")
    val PidKey = new Key("pid")
    val FidKey = new Key("fid")
    val PathKey = new Key("path")
    val DescriptionKey = new Key("description")
    val VersionKey = new Key("version")
    val UploadedByKey = new Key("uploadedBy")
    val LockKey = new Key("lock")
    val LockByKey = new Key("by", Some(LockKey))
    val LockDateKey = new Key("date", Some(LockKey))
    val IsFolderKey = new Key("isFolder")
    val AclKey = new Key("acl")
  }

  /**
   * General command responses...
   */
  object CommandStatusTypes {

    sealed trait CommandStatus[A] {
      val res: A
    }

    case class CommandOk[A](res: A) extends CommandStatus[A]

    case class CommandKo[A](res: A) extends CommandStatus[A]

    case class CommandError[A](res: A, msg: Option[String] = None) extends CommandStatus[A]

  }

}
