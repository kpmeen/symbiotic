/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.data

import controllers.converters.IdFormatters
import play.api.libs.json.{Reads, Writes}

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends Id

object FileId extends IdConverters[FileId] {
  implicit val fileIdReads: Reads[FolderId]   = reads(FileId.apply)
  implicit val fileIdWrites: Writes[FolderId] = writes

  lazy val empty: FileId = FileId("")

  override implicit def asId(s: String): FileId = FileId(s)
}
