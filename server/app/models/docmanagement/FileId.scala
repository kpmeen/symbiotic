/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import core.converters.IdConverters
import models.base.Id

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends Id

object FileId extends IdConverters[FileId] {
  implicit val fileIdReads  = reads(FileId.apply)
  implicit val fileIdWrites = writes

  lazy val empty: FileId = FileId("")

  override implicit def asId(s: String): FileId = FileId(s)
}
