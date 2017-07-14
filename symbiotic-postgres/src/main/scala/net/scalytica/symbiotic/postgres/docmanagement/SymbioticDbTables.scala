package net.scalytica.symbiotic.postgres.docmanagement

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}
import net.scalytica.symbiotic.api.types.ResourceOwner.{
  OrgOwner,
  Owner,
  OwnerType,
  UserOwner
}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.json.MetadataImplicits
import net.scalytica.symbiotic.postgres.{FilesTableName, SymbioticDb}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

trait SymbioticDbTables extends MetadataImplicits { self: SymbioticDb =>

  import profile.api._

  val filesTable = TableQuery[FileTable]

  // see src/main/resources/sql/symbiotic-create-db.sql
  type FileRow = (
      Option[UUID],
      FileId, // fileId
      Int, // version
      String, // fileName
      Path, // folder path
      Boolean, // isFolder
      Option[String], // content type
      Option[Long], // length / file size in bytes
      Option[String], // owner_id
      Option[OwnerType], // owner_type
      Option[DateTime], // uploadDate
      Option[UserId], // uploadedBy
      Option[String], // description
      Option[UserId], // lockedBy,
      Option[DateTime], // lockedDate,
      Option[JsValue] // custom_metadata
  )

  class FileTable(
      val tag: Tag
  ) extends Table[FileRow](tag, Some(dbSchema), FilesTableName) {

    val id             = column[UUID]("id", O.PrimaryKey, O.AutoInc)
    val fileId         = column[FileId]("file_id")
    val version        = column[Version]("version")
    val fileName       = column[String]("file_name")
    val path           = column[Path]("path")
    val isFolder       = column[Boolean]("is_folder")
    val contentType    = column[Option[String]]("content_type")
    val length         = column[Option[Long]]("length")
    val ownerId        = column[Option[String]]("owner_id")
    val ownerType      = column[Option[OwnerType]]("owner_type")
    val uploadDate     = column[Option[DateTime]]("upload_date")
    val uploadedBy     = column[Option[UserId]]("uploaded_by")
    val description    = column[Option[String]]("description")
    val lockedBy       = column[Option[UserId]]("locked_by")
    val lockedDate     = column[Option[DateTime]]("locked_date")
    val customMetadata = column[Option[JsValue]]("custom_metadata")

    // scalastyle:off
    override def * =
      (
        id.?,
        fileId,
        version,
        fileName,
        path,
        isFolder,
        contentType,
        length,
        ownerId,
        ownerType,
        uploadDate,
        uploadedBy,
        description,
        lockedBy,
        lockedDate,
        customMetadata
      )

    // scalastyle:on

  }

  implicit def folderToRow(f: Folder): FileRow = {
    (
      f.id,
      f.metadata.fid.getOrElse(FileId.create()),
      f.metadata.version,
      f.filename,
      f.metadata.path.getOrElse(Path.root),
      f.metadata.isFolder.getOrElse(true),
      f.fileType,
      f.length.map(_.toLong),
      f.metadata.owner.map(_.id.value),
      f.metadata.owner.map(_.ownerType),
      f.uploadDate,
      f.metadata.uploadedBy,
      f.metadata.description,
      f.metadata.lock.map(_.by),
      f.metadata.lock.map(_.date),
      f.metadata.extraAttributes.map(Json.toJson[MetadataMap])
    )
  }

  private def toOwner(
      oid: Option[String],
      ot: Option[OwnerType]
  ): Option[Owner] =
    for {
      ownerId   <- oid
      ownerType <- ot
    } yield
      ownerType match {
        case UserOwner => Owner(UserId.asId(ownerId), UserOwner)
        case OrgOwner  => Owner(OrgId.asId(ownerId), OrgOwner)
      }

  implicit def rowToFolder(row: FileRow): Folder = {
    Folder(
      id = row._1,
      filename = row._4,
      fileType = row._7,
      metadata = ManagedMetadata(
        owner = toOwner(row._9, row._10),
        fid = Option(row._2),
        isFolder = Option(row._6),
        path = Option(row._5),
        description = row._7,
        extraAttributes = row._16.map(_.as[MetadataMap])
      )
    )
  }

  implicit def fileToRow(f: File): FileRow = {
    (
      f.id,
      f.metadata.fid.getOrElse(FileId.create()),
      f.metadata.version,
      f.filename,
      f.metadata.path.getOrElse(Path.root),
      f.metadata.isFolder.getOrElse(false),
      f.fileType,
      f.length.map(_.toLong),
      f.metadata.owner.map(_.id.value),
      f.metadata.owner.map(_.ownerType),
      f.uploadDate,
      f.metadata.uploadedBy,
      f.metadata.description,
      f.metadata.lock.map(_.by),
      f.metadata.lock.map(_.date),
      f.metadata.extraAttributes.map(metadataMapToJson)
    )
  }

  def metadataMapToJson(mm: MetadataMap): JsValue = {
    Json.toJson[MetadataMap](mm)
  }

  implicit def optRowAsOptFolderF(
      maybeRowF: Future[Option[FileRow]]
  )(implicit ec: ExecutionContext): Future[Option[Folder]] =
    maybeRowF.map(_.map(rowToFolder))

  implicit def rowToFile(row: FileRow): File = {
    File(
      id = row._1,
      filename = row._4,
      fileType = row._7,
      length = row._8.map(_.toString),
      uploadDate = row._11,
      metadata = ManagedMetadata(
        owner = toOwner(row._9, row._10),
        fid = Option(row._2),
        uploadedBy = row._12,
        version = row._3,
        isFolder = Option(row._6),
        path = Option(row._5),
        description = row._13,
        lock = row._14.flatMap(by => row._15.map(date => Lock(by, date))),
        extraAttributes = row._16.flatMap(_.asOpt[MetadataMap])
      )
    )
  }

  implicit def optRowAsOptFileF(
      maybeRowF: Future[Option[FileRow]]
  )(implicit ec: ExecutionContext): Future[Option[File]] =
    maybeRowF.map(_.map(rowToFile))

  def rowToManagedFile(row: FileRow): ManagedFile = {
    val isFolder = row._6
    if (isFolder) rowToFolder(row)
    else rowToFile(row)
  }

}
