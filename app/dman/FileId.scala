/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import core.converters.IdConverters
import models.base.Id
import org.bson.types.ObjectId

sealed trait DmanId extends Id {

  override def assertId() = assert(
    assertion = ObjectId.isValid(value),
    message = "Value is not a valid format"
  )

}

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends DmanId

object FileId extends IdConverters[FileId] {
  implicit val fileIdReads = reads(FileId.apply)
  implicit val fileIdWrites = writes

  override implicit def asId(s: String): FileId = FileId(s)

  implicit def asId(oid: ObjectId): FileId = FileId(oid.toString)

  implicit def asMaybeId(moid: Option[ObjectId]): Option[FileId] = moid.map(asId)

  implicit def asObjId(fid: FileId): ObjectId = new ObjectId(fid.value)

  implicit def asMaybeObjId(ms: Option[FileId]): Option[ObjectId] = ms.map(asObjId)
}