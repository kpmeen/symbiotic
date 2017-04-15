package net.scalytica.symbiotic.play.json

import net.scalytica.symbiotic.data.MetadataKeys._
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait ManagedFileFormatters {
  implicit val mfReads: Reads[ManagedFile] = Reads { value: JsValue =>
    val isFolder = (value \ "isFolder").asOpt[Boolean].getOrElse(false)
    if (isFolder) Json.fromJson[Folder](value)(FolderFormatters.reads)
    else Json.fromJson[File](value)(FileFormatters.reads)
  }

  implicit val mfWrites: Writes[ManagedFile] = Writes {
    case f: Folder => Json.toJson[Folder](f)(FolderFormatters.writes)
    case fw: File  => Json.toJson[File](fw)(FileFormatters.writes)
  }

  implicit val metadataFormat: Format[ManagedFileMetadata] = (
    (__ \ OwnerKey.key).formatNullable[UserId] and
      (__ \ FidKey.key).formatNullable[FileId] and
      (__ \ UploadedByKey.key).formatNullable[UserId] and
      (__ \ VersionKey.key).format[Version] and
      (__ \ IsFolderKey.key).formatNullable[Boolean] and
      (__ \ PathKey.key).formatNullable[Path] and
      (__ \ DescriptionKey.key).formatNullable[String] and
      (__ \ LockKey.key).formatNullable[Lock]
  )(ManagedFileMetadata.apply, unlift(ManagedFileMetadata.unapply))

  implicit val lockFormat: Format[Lock] = Json.format[Lock]
}

object FolderFormatters {
  implicit val reads: Reads[Folder] = (
    (__ \ IdKey.key).readNullable[FolderId] and
      (__ \ "filename").read[String].or(Reads.pure("")) and
      (__ \ "metadata").read[ManagedFileMetadata]
  )((id, fn, md) => Folder(id, fn, md))

  implicit val writes: Writes[Folder] = (
    (__ \ IdKey.key).writeNullable[FolderId] and
      (__ \ "filename").write[String] and
      (__ \ "metadata").write[ManagedFileMetadata]
  )(unlift(Folder.unapply))
}

object FileFormatters extends DateTimeFormatters {

  implicit val reads: Reads[File] = (
    (__ \ IdKey.key).readNullable[FileId] and
      (__ \ "filename").read[String] and
      (__ \ "contentType").readNullable[String] and
      (__ \ "uploadDate").readNullable[DateTime] and
      (__ \ "length").readNullable[String] and
      (__ \ "stream").readIgnore[FileStream] and
      (__ \ "metadata").read[ManagedFileMetadata]
  )(File.apply _)

  implicit val writes: Writes[File] = (
    (__ \ IdKey.key).writeNullable[FileId] and
      (__ \ "filename").write[String] and
      (__ \ "contentType").writeNullable[String] and
      (__ \ "uploadDate").writeNullable[DateTime] and
      (__ \ "length").writeNullable[String] and
      (__ \ "stream").writeIgnore[FileStream] and
      (__ \ "metadata").write[ManagedFileMetadata]
  )(unlift(File.unapply))
}
