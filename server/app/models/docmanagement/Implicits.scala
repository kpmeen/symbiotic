/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import core.converters.DateTimeConverters
import models.docmanagement.MetadataKeys._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Implicits {

  object Defaults {
    implicit val fileReads: Reads[BaseFile] = Reads {
      case value: JsValue =>
        val isFolder = (value \ "isFolder").asOpt[Boolean].getOrElse(false)
        if (isFolder) Json.fromJson[Folder](value)(FolderImplicits.reads)
        else Json.fromJson[File](value)(FileImplicits.reads)
    }

    implicit val fileWrites: Writes[BaseFile] = Writes {
      case f: Folder => Json.toJson[Folder](f)(FolderImplicits.writes)
      case fw: File => Json.toJson[File](fw)(FileImplicits.writes)
    }
  }

  object FolderImplicits {
    implicit val reads: Reads[Folder] = (
      (__ \ IdKey.key).readNullable[FileId] and
      (__ \ "metadata").read[FileMetadata]
    )((id, md) => Folder.apply(id, md))

    implicit val writes: Writes[Folder] = (
      (__ \ IdKey.key).writeNullable[FileId] and
      (__ \ "metadata").write[FileMetadata]
    )(unlift(Folder.unapply))
  }

  object FileImplicits extends DateTimeConverters {
    implicit val reads: Reads[File] = (
      (__ \ IdKey.key).readNullable[FileId] and
      (__ \ "filename").read[String] and
      (__ \ "contentType").readNullable[String] and
      (__ \ "uploadDate").readNullable[DateTime] and
      (__ \ "length").readNullable[String] and
      (__ \ "stream").readNullable[FileStream](null) and
      (__ \ "metadata").read[FileMetadata]
    )(File.apply _)

    implicit val writes: Writes[File] = (
      (__ \ IdKey.key).writeNullable[FileId] and
      (__ \ "filename").write[String] and
      (__ \ "contentType").writeNullable[String] and
      (__ \ "uploadDate").writeNullable[DateTime] and
      (__ \ "length").writeNullable[String] and
      (__ \ "stream").writeNullable[FileStream](Writes.apply(s => JsNull)) and
      (__ \ "metadata").write[FileMetadata]
    )(unlift(File.unapply))
  }

}
