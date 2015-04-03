/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import core.converters.WithIdConverters
import models.base.Id
import org.bson.types.ObjectId

/**
 * Id for managed files (documents)
 */
case class FileId(id: String) extends Id

object FileId extends WithIdConverters[FileId] {
  implicit val fileIdReads = reads(FileId.apply)
  implicit val fileIdWrites = writes

  override implicit def asId(s: String): FileId = FileId(s)
}