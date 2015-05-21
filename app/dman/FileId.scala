/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import core.converters.WithDBIdConverters
import models.base.DBId

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends DBId

object FileId extends WithDBIdConverters[FileId] {
  implicit val fileIdReads = reads(FileId.apply)
  implicit val fileIdWrites = writes

  override implicit def asId(s: String): FileId = FileId(s)
}