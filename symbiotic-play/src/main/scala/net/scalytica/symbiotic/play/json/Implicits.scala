package net.scalytica.symbiotic.play.json

import java.util.UUID

import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.{
  UserStamp,
  VersionStamp
}
import net.scalytica.symbiotic.api.types.{PathNode, _}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Implicits extends SymbioticImplicits

trait JodaImplicits {
  val defaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String  = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter: Format[DateTime] = Format[DateTime](
    Reads
      .jodaDateReads(defaultReadDateTimePattern)
      .orElse(Reads.jodaDateReads(readDateTimeMillisPattern)),
    Writes.jodaDateWrites(defaultReadDateTimePattern)
  )
}

trait SymbioticImplicits extends JodaImplicits {

  implicit object FileIdFormat extends IdFormat[FileId] {
    override implicit def asId(s: String): FileId = FileId(s)
  }

  implicit def userStampFormat(
      implicit uidf: Format[UserId]
  ): Format[UserStamp] =
    (
      (__ \ "date").format[DateTime] and
        (__ \ "by").format[UserId]
    )(UserStamp.apply, unlift(UserStamp.unapply))

  implicit def versionStampFormat(
      implicit uidf: Format[UserId]
  ): Format[VersionStamp] =
    (
      (__ \ "version").format[Int] and
        (__ \ "created").formatNullable[UserStamp] and
        (__ \ "modified").formatNullable[UserStamp]
    )(VersionStamp.apply, unlift(VersionStamp.unapply))

  implicit object PathFormatters extends Format[Path] {
    override def writes(p: Path) = JsString(Path.toDisplay(p))

    override def reads(json: JsValue) = json.validateOpt[String] match {
      case JsSuccess(v, p) => JsSuccess(Path.fromDisplay(v))
      case err: JsError    => err
    }
  }

  implicit val pathNodeFormat: Format[PathNode] = Json.format[PathNode]

  implicit def lockFormat(
      implicit uidf: Format[UserId]
  ): Format[Lock] =
    (
      (__ \ "by").format[UserId] and
        (__ \ "date").format[DateTime]
    )(Lock.apply, unlift(Lock.unapply))

  implicit def metadataFormat(
      implicit uidf: Format[UserId]
  ): Format[ManagedFileMetadata] =
    (
      (__ \ OwnerKey.key).formatNullable[UserId] and
        (__ \ FidKey.key).formatNullable[FileId] and
        (__ \ UploadedByKey.key).formatNullable[UserId] and
        (__ \ VersionKey.key).format[Version] and
        (__ \ IsFolderKey.key).formatNullable[Boolean] and
        (__ \ PathKey.key).formatNullable[Path] and
        (__ \ DescriptionKey.key).formatNullable[String] and
        (__ \ LockKey.key).formatNullable[Lock]
    )(ManagedFileMetadata.apply, unlift(ManagedFileMetadata.unapply))

  def fileFormat(
      implicit uidf: Format[UserId]
  ): Format[File] =
    (
      (__ \ IdKey.key).formatNullable[UUID] and
        (__ \ "filename").format[String] and
        (__ \ "contentType").formatNullable[String] and
        (__ \ "uploadDate").formatNullable[DateTime] and
        (__ \ "length").formatNullable[String] and
        (__ \ "stream").formatIgnore[FileStream] and
        (__ \ "metadata").format[ManagedFileMetadata](metadataFormat)
    )(File.apply, unlift(File.unapply))

  def folderFormat(
      implicit uidf: Format[UserId]
  ): Format[Folder] =
    (
      (__ \ IdKey.key).formatNullable[UUID] and
        (__ \ "filename")
          .formatNullable[String]
          .inmap[String](_.getOrElse(""), Option.apply) and
        (__ \ "metadata").format[ManagedFileMetadata]
    )((id, fn, md) => Folder(id, fn, md), unlift(Folder.unapply))

  implicit def managedFileReads(
      implicit uidf: Format[UserId]
  ): Reads[ManagedFile] = Reads { value: JsValue =>
    val isFolder = (value \ "isFolder").asOpt[Boolean].getOrElse(false)
    if (isFolder) Json.fromJson[Folder](value)(folderFormat(uidf))
    else Json.fromJson[File](value)(fileFormat(uidf))
  }

  implicit def managedFileWrites(
      implicit uidf: Format[UserId]
  ): Writes[ManagedFile] = Writes {
    case f: Folder => Json.toJson[Folder](f)(folderFormat(uidf))
    case fw: File  => Json.toJson[File](fw)(fileFormat(uidf))
  }

}
