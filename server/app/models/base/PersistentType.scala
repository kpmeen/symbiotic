/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import java.util.Date

import com.mongodb.casbah.commons.Imports._
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import models.base.PersistentType.VersionStamp
import models.party.PartyBaseTypes.UserId
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json._

trait PersistentType {
  val v: Option[VersionStamp]
}

object PersistentType {

  case class UserStamp(date: DateTime, by: UserId)

  object UserStamp extends DateTimeConverters with ObjectBSONConverters[UserStamp] {
    implicit val msFormat: Format[UserStamp] = Json.format[UserStamp]

    override def toBSON(x: UserStamp): DBObject =
      MongoDBObject(
        "date" -> x.date.toDate,
        "by" -> x.by.value
      )

    override def fromBSON(dbo: DBObject): UserStamp =
      UserStamp(
        date = dbo.as[Date]("date"),
        by = UserId.asId(dbo.as[String]("by"))
      )

    def create(uid: UserId): UserStamp = UserStamp(DateTime.now, uid)
  }

  case class VersionStamp(
    version: Int = 1,
    created: Option[UserStamp] = None,
    modified: Option[UserStamp] = None
  )

  object VersionStamp extends ObjectBSONConverters[VersionStamp] {
    implicit val vsFormat: Format[VersionStamp] = Json.format[VersionStamp]

    override def toBSON(x: VersionStamp): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "version" -> x.version
      x.created.foreach(b += "created" -> UserStamp.toBSON(_))
      x.modified.foreach(b += "modified" -> UserStamp.toBSON(_))

      b.result()
    }

    override def fromBSON(dbo: DBObject): VersionStamp =
      VersionStamp(
        version = dbo.getAsOrElse[Int]("version", 0),
        created = dbo.getAs[DBObject]("created").map(UserStamp.fromBSON),
        modified = dbo.getAs[DBObject]("modified").map(UserStamp.fromBSON)
      )
  }

}

trait PersistentTypeConverters {
  implicit val oidReads: Reads[ObjectId] = __.read[String].map(s => new ObjectId(s))
  implicit val oidWrites: Writes[ObjectId] = Writes {
    (a: ObjectId) => JsString(a.toString)
  }
}
