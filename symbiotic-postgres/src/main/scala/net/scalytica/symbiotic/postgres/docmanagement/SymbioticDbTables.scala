package net.scalytica.symbiotic.postgres.docmanagement

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.json.{MetadataImplicits, PartyImplicits}
import net.scalytica.symbiotic.postgres.{FilesTableName, SymbioticDb}
import net.scalytica.symbiotic.time.SymbioticDateTime._
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

trait SymbioticDbTables extends MetadataImplicits with PartyImplicits {
  self: SymbioticDb =>

  import profile.api._

  val filesTable = TableQuery[FilesTable]

  // see src/main/resources/sql/symbiotic-create-db.sql
  type FileRow = (
      Option[UUID],
      FileId, // fileId
      Int, // version
      String, // fileName
      Path, // folder path
      Boolean, // isFolder
      Boolean, // isDeleted
      JsValue, // accessible_by
      Option[String], // content type
      Option[Long], // length / file size in bytes
      Option[String], // owner_id
      Option[Type], // owner_type
      Option[DateTime], // uploadDate
      Option[UserId], // uploadedBy
      Option[String], // description
      Option[UserId], // lockedBy,
      Option[DateTime], // lockedDate,
      Option[JsValue] // custom_metadata
  )

  type FileQuery = Query[FilesTable, FileRow, Seq]

  class FilesTable(
      val tag: Tag
  ) extends Table[FileRow](tag, Some(dbSchema), FilesTableName) {

    val id             = column[UUID]("id", O.PrimaryKey, O.AutoInc)
    val fileId         = column[FileId]("file_id")
    val version        = column[Version]("version")
    val fileName       = column[String]("file_name")
    val path           = column[Path]("path")
    val isFolder       = column[Boolean]("is_folder")
    val isDeleted      = column[Boolean]("is_deleted")
    val accessibleBy   = column[JsValue]("accessible_by")
    val contentType    = column[Option[String]]("content_type")
    val length         = column[Option[Long]]("length")
    val ownerId        = column[Option[String]]("owner_id")
    val ownerType      = column[Option[Type]]("owner_type")
    val createdDate    = column[Option[DateTime]]("created_date")
    val createdBy      = column[Option[UserId]]("created_by")
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
        isDeleted,
        accessibleBy,
        contentType,
        length,
        ownerId,
        ownerType,
        createdDate,
        createdBy,
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
      f.metadata.isFolder,
      f.metadata.isDeleted,
      Json.toJson(f.metadata.accessibleBy),
      f.fileType,
      f.length.map(_.toLong),
      f.metadata.owner.map(_.id.value),
      f.metadata.owner.map(_.tpe),
      f.createdDate,
      f.metadata.createdBy,
      f.metadata.description,
      f.metadata.lock.map(_.by),
      f.metadata.lock.map(_.date),
      f.metadata.extraAttributes.map(metadataMapToJson)
    )
  }

  private[this] def toOwner(
      oid: Option[String],
      ot: Option[Type]
  ): Option[Owner] =
    for {
      ownerId   <- oid
      ownerType <- ot
    } yield
      ownerType match {
        case Usr => Owner(UserId.asId(ownerId), Usr)
        case Org => Owner(OrgId.asId(ownerId), Org)
      }

  implicit def rowToFolder(row: FileRow): Folder = {
    Folder(
      id = row._1,
      filename = row._4,
      fileType = row._9,
      createdDate = row._13,
      metadata = ManagedMetadata(
        owner = toOwner(row._11, row._12),
        fid = Option(row._2),
        createdBy = row._14,
        version = row._3,
        isFolder = row._6,
        isDeleted = row._7,
        path = Option(row._5),
        description = row._15,
        lock = row._16.flatMap(by => row._17.map(date => Lock(by, date))),
        accessibleBy = row._8.as[Seq[AllowedParty]],
        extraAttributes = row._18.flatMap(_.asOpt[MetadataMap])
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
      f.metadata.isFolder,
      f.metadata.isDeleted,
      Json.toJson(f.metadata.accessibleBy),
      f.fileType,
      f.length.map(_.toLong),
      f.metadata.owner.map(_.id.value),
      f.metadata.owner.map(_.tpe),
      f.createdDate.orElse(Option(now)),
      f.metadata.createdBy,
      f.metadata.description,
      f.metadata.lock.map(_.by),
      f.metadata.lock.map(_.date),
      f.metadata.extraAttributes.map(metadataMapToJson)
    )
  }

  def metadataMapToJson(mm: MetadataMap): JsValue = Json.toJson[MetadataMap](mm)

  implicit def optRowAsOptFolderF(
      maybeRowF: Future[Option[FileRow]]
  )(implicit ec: ExecutionContext): Future[Option[Folder]] =
    maybeRowF.map(_.map(rowToFolder))

  implicit def rowToFile(row: FileRow): File = {
    File(
      id = row._1,
      filename = row._4,
      fileType = row._9,
      length = row._10.map(_.toString),
      createdDate = row._13,
      metadata = ManagedMetadata(
        owner = toOwner(row._11, row._12),
        fid = Option(row._2),
        createdBy = row._14,
        version = row._3,
        isFolder = row._6,
        isDeleted = row._7,
        path = Option(row._5),
        description = row._15,
        lock = row._16.flatMap(by => row._17.map(date => Lock(by, date))),
        accessibleBy = row._8.as[Seq[AllowedParty]],
        extraAttributes = row._18.flatMap(_.asOpt[MetadataMap])
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
