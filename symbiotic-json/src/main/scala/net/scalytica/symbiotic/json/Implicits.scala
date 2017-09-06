package net.scalytica.symbiotic.json

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}
import net.scalytica.symbiotic.api.types.PersistentType.{
  UserStamp,
  VersionStamp
}
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}
import net.scalytica.symbiotic.api.types.{PathNode, _}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Implicits extends SymbioticImplicits

/**
 * Symbiotic specific picklers for Joda DateTime.
 */
trait JodaImplicits {
  val defaultDateTimePattern: String  = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  val dateTimeNoMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"

  /** Joda date formatters */
  implicit val dateTimeFormatter: Format[DateTime] = Format[DateTime](
    JodaReads
      .jodaDateReads(defaultDateTimePattern)
      .orElse(JodaReads.jodaDateReads(dateTimeNoMillisPattern)),
    JodaWrites.jodaDateWrites(defaultDateTimePattern)
  )
}

/**
 * Implicits for handling JSON pickling of [[MetadataValue]]s encoded
 * in the [[MetadataMap]] type
 */
trait MetadataImplicits extends JodaImplicits {

  implicit object StrMetadataValueFormat extends Format[StrValue] {
    override def writes(o: StrValue) = JsString(o.value)

    override def reads(json: JsValue): JsResult[StrValue] =
      json.validate[String] match {
        case JsSuccess(v, _) => JsSuccess(StrValue(v))
        case err: JsError    => err
      }
  }

  implicit object IntMetadataValueFormat extends Format[IntValue] {
    override def writes(o: IntValue) = JsNumber(o.value)

    override def reads(json: JsValue): JsResult[IntValue] =
      json.validate[Int] match {
        case JsSuccess(v, _) => JsSuccess(IntValue(v))
        case err: JsError    => err
      }
  }

  implicit object LongMetadataValueFormat extends Format[LongValue] {
    override def writes(o: LongValue) = JsNumber(o.value)

    override def reads(json: JsValue): JsResult[LongValue] =
      json.validate[Long] match {
        case JsSuccess(v, _) => JsSuccess(LongValue(v))
        case err: JsError    => err
      }
  }

  implicit object DoubleMetadataValueFormat extends Format[DoubleValue] {
    override def writes(o: DoubleValue) = JsNumber(o.value)

    override def reads(json: JsValue): JsResult[DoubleValue] =
      json.validate[Double] match {
        case JsSuccess(v, _) => JsSuccess(DoubleValue(v))
        case err: JsError    => err
      }
  }

  implicit object BoolMetadataValueFormat extends Format[BoolValue] {
    override def writes(o: BoolValue) = JsBoolean(o.value)

    override def reads(json: JsValue): JsResult[BoolValue] =
      json.validate[Boolean] match {
        case JsSuccess(v, _) => JsSuccess(BoolValue(v))
        case err: JsError    => err
      }
  }

  implicit object JodaMetadataValueFormat extends Format[JodaValue] {
    override def writes(o: JodaValue): JsValue = Json.toJson[DateTime](o.value)

    override def reads(json: JsValue): JsResult[JodaValue] =
      json.validate[DateTime] match {
        case JsSuccess(v, _) => JsSuccess(JodaValue(v))
        case err: JsError    => err
      }
  }

  implicit object MetadataMapFormat extends Format[MetadataMap] {

    def handleJsonString(s: JsString): MetadataValue[_] =
      s.validate[DateTime].map(JodaValue.apply).getOrElse(StrValue(s.value))

    def handleJsonNumber(bd: BigDecimal): MetadataValue[_] =
      if (bd.isValidInt) IntValue(bd.toInt)
      else if (bd.isValidLong) LongValue(bd.toLong)
      else if (bd.isDecimalDouble) DoubleValue(bd.toDouble)
      else EmptyValue

    // scalastyle:off line.size.limit cyclomatic.complexity
    override def writes(o: MetadataMap): JsObject = {
      o.foldLeft(JsObject.empty) {
        case (jso, (key, value: StrValue)) =>
          jso ++ Json.obj(key -> Json.toJson[StrValue](value))

        case (jso, (key, value: IntValue)) =>
          jso ++ Json.obj(key -> Json.toJson[IntValue](value))

        case (jso, (key, value: LongValue)) =>
          jso ++ Json.obj(key -> Json.toJson[LongValue](value))

        case (jso, (key, value: DoubleValue)) =>
          jso ++ Json.obj(key -> Json.toJson[DoubleValue](value))

        case (jso, (key, value: BoolValue)) =>
          jso ++ Json.obj(key -> Json.toJson[BoolValue](value))

        case (jso, (key, value: JodaValue)) =>
          jso ++ Json.obj(key -> Json.toJson[JodaValue](value))

        case (jso, (key, EmptyValue)) =>
          jso ++ Json.obj(key -> JsNull)
      }
    }

    override def reads(json: JsValue): JsResult[MetadataMap] = {
      json.validate[JsObject] match {
        case JsSuccess(jso, _) =>
          val mdm = jso.value.map {
            case (k, JsNumber(num))   => k -> handleJsonNumber(num)
            case (k, JsBoolean(bool)) => k -> BoolValue(bool)
            case (k, s: JsString)     => k -> handleJsonString(s)
            case (k, JsNull)          => k -> EmptyValue
          }.toSeq
          JsSuccess(MetadataMap(mdm: _*))

        case err: JsError => err
      }
    }

    // scalastyle:on line.size.limit cyclomatic.complexity
  }

}

trait PartyImplicits extends JodaImplicits {

  implicit def defaultUserIdFormat: Format[UserId] = new Format[UserId] {
    override def writes(o: UserId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[UserId] =
      json.validate[String].map(UserId.apply)
  }

  implicit def defaultOrgIdFormat: Format[OrgId] = new Format[OrgId] {
    override def writes(o: OrgId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[OrgId] =
      json.validate[String].map(OrgId.apply)
  }

  implicit val userStampFormat: Format[UserStamp] = Json.format[UserStamp]

  implicit val versionStampFormat: Format[VersionStamp] =
    Json.format[VersionStamp]

  implicit val ownerFormat: Format[Owner] = (
    (__ \ OwnerIdKey.key).format[String] and
      (__ \ OwnerTypeKey.key).format[String]
  )(Owner.apply, o => (o.id.value, o.tpe.tpe))

  implicit val allowedPartyFormat: Format[AllowedParty] = (
    (__ \ AccessibleByIdKey.key).format[String] and
      (__ \ AccessibleByTpeKey.key).format[String]
  )(AllowedParty.apply, p => (p.id.value, p.tpe.tpe))

}

/**
 * Symbiotic specific JSON formatter implicits. Most of these require an
 * implicit {{{Writes[UserId]}}} to be in scope when used. Since Symbiotic only
 * provides {{{UserId}}}as a trait, it is up to the consumer of this library to
 * ensure that the UserId formatter is provided.
 */
trait SymbioticImplicits
    extends PartyImplicits
    with JodaImplicits
    with MetadataImplicits {

  implicit def readsToIgnoreReads[T](r: JsPath): IgnoreJsPath = IgnoreJsPath(r)

  implicit object FileIdFormat extends IdFormat[FileId] {
    override implicit def asId(s: String): FileId = FileId(s)
  }

  implicit object PathFormatters extends Format[Path] {
    override def writes(p: Path) = JsString(Path.toDisplay(p))

    override def reads(json: JsValue): JsResult[Path] =
      json.validateOpt[String] match {
        case JsSuccess(v, _) => JsSuccess(Path.fromDisplay(v))
        case err: JsError    => err
      }
  }

  implicit val pathNodeFormat: Format[PathNode] = Json.format[PathNode]

  implicit val lockFormat: Format[Lock] = Json.format[Lock]

  implicit val metadataFormat: Format[ManagedMetadata] = (
    (__ \ OwnerKey.key).formatNullable[Owner] and
      (__ \ AccessibleByKey.key).format[Seq[AllowedParty]] and
      (__ \ FidKey.key).formatNullable[FileId] and
      (__ \ UploadedByKey.key).formatNullable[UserId] and
      (__ \ VersionKey.key).format[Version] and
      (__ \ IsFolderKey.key).formatNullable[Boolean] and
      (__ \ PathKey.key).formatNullable[Path] and
      (__ \ DescriptionKey.key).formatNullable[String] and
      (__ \ LockKey.key).formatNullable[Lock] and
      (__ \ ExtraAttributesKey.key).formatNullable[MetadataMap]
  )(ManagedMetadata.apply, unlift(ManagedMetadata.unapply))

  val fileFormat: Format[File] = (
    (__ \ IdKey.key).formatNullable[UUID] and
      (__ \ "filename").format[String] and
      (__ \ "contentType").formatNullable[String] and
      (__ \ "uploadDate").formatNullable[DateTime] and
      (__ \ "length").formatNullable[String] and
      (__ \ "stream").formatIgnore[FileStream] and
      (__ \ "metadata").format[ManagedMetadata]
  )(File.apply, unlift(File.unapply))

  val folderFormat: Format[Folder] = (
    (__ \ IdKey.key).formatNullable[UUID] and
      (__ \ "filename")
        .formatNullable[String]
        .inmap[String](_.getOrElse(""), Option.apply) and
      (__ \ "folderType").formatNullable[String] and
      (__ \ "createdDate").formatNullable[DateTime] and
      (__ \ "metadata").format[ManagedMetadata]
  )((id, fn, ft, cd, md) => Folder(id, fn, ft, cd, md), unlift(Folder.unapply))

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
