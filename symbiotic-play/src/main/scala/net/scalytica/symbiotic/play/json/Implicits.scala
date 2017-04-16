package net.scalytica.symbiotic.play.json

import java.util.UUID

import net.scalytica.symbiotic.api.types
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.{
  UserStamp,
  VersionStamp
}
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Implicits extends IdImplicits {

  val defaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String  = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter: Format[DateTime] = Format[DateTime](
    Reads
      .jodaDateReads(defaultReadDateTimePattern)
      .orElse(Reads.jodaDateReads(readDateTimeMillisPattern)),
    Writes.jodaDateWrites(defaultReadDateTimePattern)
  )

  implicit val uuidFormat: Format[UUID] = Format(
    fjs = __.read[String].map(s => UUID.fromString(s)),
    tjs = Writes(a => JsString(a.toString))
  )

  implicit def usrStampFormat(
      implicit userIdReads: Reads[UserId],
      userIdWrites: Writes[UserId]
  ): Format[UserStamp] = Json.format[UserStamp]

  implicit def VerStampFmt(
      implicit userIdReads: Reads[UserId],
      userIdWrites: Writes[UserId]
  ): Format[VersionStamp] = Json.format[VersionStamp]

  implicit object PathFormatters extends Format[Path] {
    override def writes(p: Path) = JsString(Path.toDisplay(p))

    override def reads(json: JsValue) = json.validateOpt[String] match {
      case JsSuccess(v, p) => JsSuccess(Path.fromDisplay(v))
      case err: JsError    => err
    }
  }

  implicit val pathNodeFormat: Format[types.PathNode] =
    Json.format[types.PathNode]

  implicit def lockReads(
      implicit userIdReads: Reads[UserId]
  ): Reads[Lock] = Json.reads[Lock]

  implicit def lockWrites(
      implicit userIdWrites: Writes[UserId]
  ): Writes[Lock] = Json.writes[Lock]

  implicit def metadataReads(
      implicit userIdFmt: Reads[UserId]
  ): Reads[ManagedFileMetadata] =
    (
      (__ \ OwnerKey.key).readNullable[UserId] and
        (__ \ FidKey.key).readNullable[FileId] and
        (__ \ UploadedByKey.key).readNullable[UserId] and
        (__ \ VersionKey.key).read[Version] and
        (__ \ IsFolderKey.key).readNullable[Boolean] and
        (__ \ PathKey.key).readNullable[Path] and
        (__ \ DescriptionKey.key).readNullable[String] and
        (__ \ LockKey.key).readNullable[Lock]
    )(ManagedFileMetadata.apply _)

  implicit def metadataWrites(
      implicit userIdWrites: Writes[UserId]
  ): Writes[ManagedFileMetadata] =
    (
      (__ \ OwnerKey.key).writeNullable[UserId] and
        (__ \ FidKey.key).writeNullable[FileId] and
        (__ \ UploadedByKey.key).writeNullable[UserId] and
        (__ \ VersionKey.key).write[Version] and
        (__ \ IsFolderKey.key).writeNullable[Boolean] and
        (__ \ PathKey.key).writeNullable[Path] and
        (__ \ DescriptionKey.key).writeNullable[String] and
        (__ \ LockKey.key).writeNullable[Lock]
    )(unlift(ManagedFileMetadata.unapply))

  implicit def managedFileReads(
      implicit userIdReads: Reads[UserId]
  ): Reads[ManagedFile] = Reads { value: JsValue =>
    val isFolder = (value \ "isFolder").asOpt[Boolean].getOrElse(false)
    if (isFolder) Json.fromJson[Folder](value)(folderReads)
    else Json.fromJson[File](value)(fileReads)
  }

  implicit def managedFileWrites(
      implicit userIdWrites: Writes[UserId]
  ): Writes[ManagedFile] = Writes {
    case f: Folder => Json.toJson[Folder](f)(folderWrites)
    case fw: File  => Json.toJson[File](fw)(fileWrites)
  }

  implicit def fileReads(
      implicit userIdReads: Reads[UserId]
  ): Reads[File] =
    (
      (__ \ IdKey.key).readNullable[FileId] and
        (__ \ "filename").read[String] and
        (__ \ "contentType").readNullable[String] and
        (__ \ "uploadDate").readNullable[DateTime] and
        (__ \ "length").readNullable[String] and
        (__ \ "stream").readIgnore[FileStream] and
        (__ \ "metadata").read[ManagedFileMetadata](metadataReads)
    )(File.apply _)

  implicit def fileWrites(
      implicit userIdWrites: Writes[UserId]
  ): Writes[File] =
    (
      (__ \ IdKey.key).writeNullable[FileId] and
        (__ \ "filename").write[String] and
        (__ \ "contentType").writeNullable[String] and
        (__ \ "uploadDate").writeNullable[DateTime] and
        (__ \ "length").writeNullable[String] and
        (__ \ "stream").writeIgnore[FileStream] and
        (__ \ "metadata").write[ManagedFileMetadata](metadataWrites)
    )(unlift(File.unapply))

  implicit def folderReads(
      implicit userIdReads: Reads[UserId]
  ): Reads[Folder] =
    (
      (__ \ IdKey.key).readNullable[FolderId] and
        (__ \ "filename").read[String].or(Reads.pure("")) and
        (__ \ "metadata").read[ManagedFileMetadata]
    )((id, fn, md) => Folder(id, fn, md))

  implicit def folderWrites(
      implicit userIdWrites: Writes[UserId]
  ): Writes[Folder] =
    (
      (__ \ IdKey.key).writeNullable[FolderId] and
        (__ \ "filename").write[String] and
        (__ \ "metadata").write[ManagedFileMetadata]
    )(unlift(Folder.unapply))

}
