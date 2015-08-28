/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import core.converters.DateTimeConverters
import dman.MetadataKeys._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Implicits {

  object Defaults {
    implicit val fileReads: Reads[BaseFile] = Reads {
      case value: JsValue =>
        val isFolder = (value \ "isFolder").asOpt[Boolean].getOrElse(false)
        if (isFolder) Json.fromJson[Folder](value)(FolderImplicits.reads)
        else Json.fromJson[FileWrapper](value)(FileWrapperImplicits.reads)
    }

    implicit val fileWrites: Writes[BaseFile] = Writes {
      case f: Folder => Json.toJson[Folder](f)(FolderImplicits.writes)
      case fw: FileWrapper => Json.toJson[FileWrapper](fw)(FileWrapperImplicits.writes)
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

  object FileWrapperImplicits extends DateTimeConverters {
    implicit val reads: Reads[FileWrapper] = (
      (__ \ IdKey.key).readNullable[FileId] and
        (__ \ "filename").read[String] and
        (__ \ "contentType").readNullable[String] and
        (__ \ "uploadDate").readNullable[DateTime] and
        (__ \ "size").readNullable[String] and
        (__ \ "stream").readNullable[FileStream](null) and
        (__ \ "metadata").read[FileMetadata]
      )(FileWrapper.apply _)

    implicit val writes: Writes[FileWrapper] = (
      (__ \ IdKey.key).writeNullable[FileId] and
        (__ \ "filename").write[String] and
        (__ \ "contentType").writeNullable[String] and
        (__ \ "uploadDate").writeNullable[DateTime] and
        (__ \ "size").writeNullable[String] and
        (__ \ "stream").writeNullable[FileStream](Writes.apply(s => JsNull)) and
        (__ \ "metadata").write[FileMetadata]
      )(unlift(FileWrapper.unapply))
  }
}
