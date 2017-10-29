package net.scalytica.symbiotic.models.dman

import play.api.libs.json.{Format, Json}

case class FileMetadata(
    // All files **must** have a FileId
    fid: String,
    owner: Option[Owner],
    createdBy: Option[String] = None,
    version: Int = 1,
    isFolder: Option[Boolean] = None,
    path: Option[String] = None,
    description: Option[String] = None,
    lock: Option[Lock] = None
)

object FileMetadata {

  implicit val format: Format[FileMetadata] = Json.format[FileMetadata]

}
