package net.scalytica.symbiotic.postgres.docmanagement

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.postgres.{FilesTableName, SymbioticDb}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

trait SymbioticDbTables extends ColumnTypeMappers { self: SymbioticDb =>

  import profile.api._

  val filesTable = TableQuery[FileTable]

  // see src/main/resources/sql/symbiotic-create-db.sql
  type FileRow = (
      // format: off
    Option[UUID],
      FileId, // fileId
      Int, // version
      String, // fileName
      Path, // folder path
      Boolean, // isFolder
      Option[String], // content type
      Option[Long], // length / file size in bytes
      Option[UserId], // owner
      Option[DateTime], // uploadDate
      Option[UserId], // uploadedBy
      Option[String], // description
      Option[UserId], // lockedBy,
      Option[DateTime] // lockedDate,
    //  Option[Map[String, Any]] ???
    // format: on
  )

  class FileTable(
      val tag: Tag
  ) extends Table[FileRow](tag, Some(dbSchema), FilesTableName) {

    val id          = column[UUID]("id", O.PrimaryKey, O.AutoInc)
    val fileId      = column[FileId]("file_id")
    val version     = column[Version]("version")
    val fileName    = column[String]("file_name")
    val path        = column[Path]("path")
    val isFolder    = column[Boolean]("is_folder")
    val contentType = column[Option[String]]("content_type")
    val length      = column[Option[Long]]("length")
    val owner       = column[Option[UserId]]("owner")
    val uploadDate  = column[Option[DateTime]]("upload_date")
    val uploadedBy  = column[Option[UserId]]("uploaded_by")
    val description = column[Option[String]]("description")
    val lockedBy    = column[Option[UserId]]("locked_by")
    val lockedDate  = column[Option[DateTime]]("locked_date")

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
        owner,
        uploadDate,
        uploadedBy,
        description,
        lockedBy,
        lockedDate
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
      f.contentType,
      f.length.map(_.toLong),
      f.metadata.owner,
      f.uploadDate,
      f.metadata.uploadedBy,
      f.metadata.description,
      f.metadata.lock.map(_.by),
      f.metadata.lock.map(_.date)
    )
  }

  implicit def rowToFolder(row: FileRow): Folder = {
    Folder(
      id = row._1,
      filename = row._4,
      metadata = ManagedFileMetadata(
        owner = row._9,
        fid = Option(row._2),
        isFolder = Option(row._6),
        path = Option(row._5),
        description = row._7
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
      f.contentType,
      f.length.map(_.toLong),
      f.metadata.owner,
      f.uploadDate,
      f.metadata.uploadedBy,
      f.metadata.description,
      f.metadata.lock.map(_.by),
      f.metadata.lock.map(_.date)
    )
  }

  implicit def optRowAsOptFolderF(
      maybeRowF: Future[Option[FileRow]]
  )(implicit ec: ExecutionContext): Future[Option[Folder]] =
    maybeRowF.map(_.map(rowToFolder))

  implicit def rowToFile(row: FileRow): File = {
    File(
      id = row._1,
      filename = row._4,
      contentType = row._7,
      length = row._8.map(_.toString),
      uploadDate = row._10,
      metadata = ManagedFileMetadata(
        owner = row._9,
        fid = Option(row._2),
        uploadedBy = row._11,
        version = row._3,
        isFolder = Option(row._6),
        path = Option(row._5),
        description = row._12,
        lock = row._13.flatMap(by => row._14.map(date => Lock(by, date)))
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
