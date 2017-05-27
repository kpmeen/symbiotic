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

  implicit def UserStampWrites(implicit w: Writes[UserId]): Writes[UserStamp] =
    new Writes[UserStamp] {
      override def writes(o: UserStamp) = Json.obj(
        "date" -> Json.toJson[DateTime](o.date),
        "by"   -> w.writes(o.by)
      )
    }

  implicit def UserStampReads(implicit r: Reads[UserId]): Reads[UserStamp] =
    new Reads[UserStamp] {
      override def reads(json: JsValue) =
        for {
          date <- (json \ "date").validate[DateTime]
          by   <- (json \ "by").validate[UserId]
        } yield UserStamp(date, by)
    }

  implicit def VersionStampWrites(
      implicit w: Writes[UserId]
  ): Writes[VersionStamp] =
    new Writes[VersionStamp] {
      override def writes(o: VersionStamp) = {
        val values = Map.newBuilder[String, JsValue]
        values += "version" -> JsNumber(o.version)
        o.created.map(c => values += "created"   -> UserStampWrites.writes(c))
        o.modified.map(m => values += "modified" -> UserStampWrites.writes(m))
        JsObject(values.result())
      }
    }

  implicit def VersionStampReads(
      implicit r: Reads[UserId]
  ): Reads[VersionStamp] =
    new Reads[VersionStamp] {
      override def reads(json: JsValue) =
        for {
          version       <- (json \ "version").validate[Int]
          maybeCreated  <- (json \ "created").validateOpt[UserStamp]
          maybeModified <- (json \ "modified").validateOpt[UserStamp]
        } yield VersionStamp(version, maybeCreated, maybeModified)
    }

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

  implicit def LockWrites(implicit w: Writes[UserId]): Writes[Lock] =
    new Writes[Lock] {
      override def writes(o: Lock) =
        Json.obj(
          "by"   -> w.writes(o.by),
          "date" -> Json.toJson[DateTime](o.date)
        )
    }

  implicit def LockReads(implicit r: Reads[UserId]): Reads[Lock] =
    new Reads[Lock] {
      override def reads(json: JsValue) =
        for {
          by   <- (json \ "by").validate[UserId]
          date <- (json \ "date").validate[DateTime]
        } yield Lock(by, date)
    }

  implicit def metadataFormat(
      implicit f: Format[UserId]
  ): Format[ManagedFileMetadata] =
    (
      (__ \ OwnerKey.key).formatNullable[UserId] and
        (__ \ FidKey.key).formatNullable[FileId] and
        (__ \ UploadedByKey.key).formatNullable[UserId] and
        (__ \ VersionKey.key).format[Version] and
        (__ \ IsFolderKey.key).formatNullable[Boolean] and
        (__ \ PathKey.key).formatNullable[Path] and
        (__ \ DescriptionKey.key).formatNullable[String] and
        (__ \ LockKey.key).formatNullable[Lock](Format(LockReads, LockWrites))
    )(ManagedFileMetadata.apply, unlift(ManagedFileMetadata.unapply))

  def fileFormat(implicit f: Format[UserId]): Format[File] =
    (
      (__ \ IdKey.key).formatNullable[UUID] and
        (__ \ "filename").format[String] and
        (__ \ "contentType").formatNullable[String] and
        (__ \ "uploadDate").formatNullable[DateTime] and
        (__ \ "length").formatNullable[String] and
        (__ \ "stream").formatIgnore[FileStream] and
        (__ \ "metadata").format[ManagedFileMetadata](metadataFormat)
    )(File.apply, unlift(File.unapply))

  def folderFormat(implicit f: Format[UserId]): Format[Folder] =
    (
      (__ \ IdKey.key).formatNullable[UUID] and
        (__ \ "filename")
          .formatNullable[String]
          .inmap[String](_.getOrElse(""), Option.apply) and
        (__ \ "metadata").format[ManagedFileMetadata]
    )((id, fn, md) => Folder(id, fn, md), unlift(Folder.unapply))

  implicit def ManagedFileFormat(
      implicit f: Format[UserId]
  ): Format[ManagedFile] = new Format[ManagedFile] {
    override def writes(o: ManagedFile): JsValue = o match {
      case folder: Folder => Json.toJson[Folder](folder)(folderFormat(f))
      case file: File     => Json.toJson[File](file)(fileFormat(f))
    }

    override def reads(json: JsValue): JsResult[ManagedFile] = {
      val isFolder = (json \ "isFolder").asOpt[Boolean].getOrElse(false)
      if (isFolder) Json.fromJson[Folder](json)(folderFormat(f))
      else Json.fromJson[File](json)(fileFormat(f))
    }
  }

}
