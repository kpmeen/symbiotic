package net.scalytica.symbiotic.mongodb.bson

import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.{
  UserStamp,
  VersionStamp
}
import org.joda.time.DateTime

object BaseBSONConverters {

  object Implicits
      extends DateTimeBSONConverter
      with VersionStampBSONConverter
      with UserStampBSONConverter

  trait DateTimeBSONConverter {
    implicit def asDateTime(jud: java.util.Date): DateTime = new DateTime(jud)

    implicit def asOptDateTime(
        maybeJud: Option[java.util.Date]
    ): Option[DateTime] =
      maybeJud.map(jud => asDateTime(jud))
  }

  trait UserStampBSONConverter extends DateTimeBSONConverter {
    implicit def userstampToBSON(x: UserStamp): DBObject =
      MongoDBObject(
        "date" -> x.date.toDate,
        "by"   -> x.by.value
      )

    implicit def userstampFromBSON(
        dbo: DBObject
    ): UserStamp =
      UserStamp(
        date = dbo.as[java.util.Date]("date"),
        by = UserId.asId(dbo.as[String]("by"))
      )
  }

  trait VersionStampBSONConverter extends UserStampBSONConverter {

    implicit def versionstampToBSON(x: VersionStamp): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "version" -> x.version
      x.created.foreach(b += "created"   -> userstampToBSON(_))
      x.modified.foreach(b += "modified" -> userstampToBSON(_))

      b.result()
    }

    implicit def versionstampFromBSON(dbo: DBObject): VersionStamp =
      VersionStamp(
        version = dbo.getAsOrElse[Int]("version", 0),
        created = dbo.getAs[DBObject]("created").map(userstampFromBSON),
        modified = dbo.getAs[DBObject]("modified").map(userstampFromBSON)
      )
  }

}
