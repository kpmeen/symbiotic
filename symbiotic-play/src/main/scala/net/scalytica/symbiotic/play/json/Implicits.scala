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
  val defaultDateTimePattern: String  = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  val dateTimeNoMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"

  /** Joda date formatters */
  implicit val dateTimeFormatter: Format[DateTime] = Format[DateTime](
    Reads
      .jodaDateReads(defaultDateTimePattern)
      .orElse(Reads.jodaDateReads(dateTimeNoMillisPattern)),
    Writes.jodaDateWrites(defaultDateTimePattern)
  )
}

/**
 * Symbiotic specific JSON formatter implicits. Most of these require an
 * implicit {{{Writes[UserId]}}} to be in scope when used. Since Symbiotic only
 * provides {{{UserId}}}as a trait, it is up to the consumer of this library to
 * ensure that the UserId formatter is provided.
 */
trait SymbioticImplicits extends JodaImplicits {

  implicit def defaultUserIdFormat: Format[UserId] = new Format[UserId] {

    override def writes(o: UserId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[UserId] =
      json.validate[String].map(UserId.apply)
  }

  implicit val userStampFormat: Format[UserStamp] = Json.format[UserStamp]

  implicit val versionStampFormat: Format[VersionStamp] =
    Json.format[VersionStamp]

  implicit object FileIdFormat extends IdFormat[FileId] {
    override implicit def asId(s: String): FileId = FileId(s)
  }

  implicit object PathFormatters extends Format[Path] {
    override def writes(p: Path) = JsString(Path.toDisplay(p))

    override def reads(json: JsValue) = json.validateOpt[String] match {
      case JsSuccess(v, p) => JsSuccess(Path.fromDisplay(v))
      case err: JsError    => err
    }
  }

  implicit val pathNodeFormat: Format[PathNode] = Json.format[PathNode]

  implicit val lockFormat: Format[Lock] = Json.format[Lock]

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

  val fileFormat: Format[File] = (
    (__ \ IdKey.key).formatNullable[UUID] and
      (__ \ "filename").format[String] and
      (__ \ "contentType").formatNullable[String] and
      (__ \ "uploadDate").formatNullable[DateTime] and
      (__ \ "length").formatNullable[String] and
      (__ \ "stream").formatIgnore[FileStream] and
      (__ \ "metadata").format[ManagedFileMetadata]
  )(File.apply, unlift(File.unapply))

  val folderFormat: Format[Folder] = (
    (__ \ IdKey.key).formatNullable[UUID] and
      (__ \ "filename")
        .formatNullable[String]
        .inmap[String](_.getOrElse(""), Option.apply) and
      (__ \ "metadata").format[ManagedFileMetadata]
  )((id, fn, md) => Folder(id, fn, md), unlift(Folder.unapply))

  implicit object ManagedFileFormat extends Format[ManagedFile] {
    override def writes(o: ManagedFile): JsValue = o match {
      case folder: Folder => folderFormat.writes(folder)
      case file: File     => fileFormat.writes(file)
    }

    override def reads(json: JsValue): JsResult[ManagedFile] = {
      val isFolder =
        (json \ "metadata" \ "isFolder").asOpt[Boolean].getOrElse(false)

      if (isFolder) folderFormat.reads(json)
      else fileFormat.reads(json)
    }
  }

}
