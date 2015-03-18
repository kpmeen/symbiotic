/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.docmanagement

import com.mongodb.casbah.Imports._
import core.converters.{WithDateTimeConverters, WithIdConverters}
import models.base.Id
import models.parties.UserId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

/**
 * Id for managed files (documents)
 */
case class FileId(id: ObjectId) extends Id

object FileId extends WithIdConverters[FileId] {
  implicit val fileIdReads = reads(FileId.apply)
  implicit val fileIdWrites = writes

  override implicit def asId(oid: ObjectId): FileId = FileId(oid)
}

/**
 * Used for handling (un)locking files for change (or version incrementation)
 */
case class Lock(by: UserId, date: DateTime)

object Lock extends WithDateTimeConverters {
  implicit val lockFormat: Format[Lock] = Json.format[Lock]

  implicit def toBSON(lock: Lock): MongoDBObject = {
    MongoDBObject(
      "by" -> lock.by.id,
      "date" -> lock.date.toDate
    )
  }

  implicit def fromBSON(dbo: MongoDBObject): Lock = {
    Lock(
      by = dbo.as[ObjectId]("by"),
      date = dbo.as[java.util.Date]("date")
    )
  }

  object LockOpStatusTypes {

    sealed trait LockOpStatus[A]

    case class Success[A](res: A) extends LockOpStatus[A]
    case class Locked[A](res: UserId) extends LockOpStatus[A]
    case class NotAllowed[A]() extends LockOpStatus[A]
    case class NotLocked[A]() extends LockOpStatus[A]
    case class Error[A](reason: String) extends LockOpStatus[A]

  }
}