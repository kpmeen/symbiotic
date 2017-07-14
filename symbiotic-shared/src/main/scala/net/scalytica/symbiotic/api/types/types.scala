package net.scalytica.symbiotic.api

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}

import scala.concurrent.Future

package object types {

  // Defining some useful type aliases
  type FileStream = Source[ByteString, Future[IOResult]]
  type Version    = Int
  type FolderId   = FileId

  type TransUserId = String => UserId
  type TransOrgId  = String => OrgId

  /**
   * Key definitions for metadata content as the are represented in JSON.
   */
  object MetadataKeys {
    val MetadataKey = "metadata"

    case class Key(key: String, parent: Option[Key] = None) {

      def full: String = {
        parent.map(p => s"${p.full}.$key").getOrElse(s"$MetadataKey.$key")
      }

      def partial: String = parent.map(p => s"${p.key}").getOrElse(key)
    }

    val IdKey              = Key("id")
    val OwnerKey           = Key("owner")
    val OwnerIdKey         = Key("ownerId", Some(OwnerKey))
    val OwnerTypeKey       = Key("ownerType", Some(OwnerKey))
    val FidKey             = Key("fid")
    val PathKey            = Key("path")
    val DescriptionKey     = Key("description")
    val VersionKey         = Key("version")
    val UploadedByKey      = Key("uploadedBy")
    val LockKey            = Key("lock")
    val ExtraAttributesKey = Key("extraAttributes")
    val LockByKey          = Key("by", Some(LockKey))
    val LockDateKey        = Key("date", Some(LockKey))
    val IsFolderKey        = Key("isFolder")
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

    case class CommandError[A](
        res: A,
        msg: Option[String] = None
    ) extends CommandStatus[A]

  }

}
