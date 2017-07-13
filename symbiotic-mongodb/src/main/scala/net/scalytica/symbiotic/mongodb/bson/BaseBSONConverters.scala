package net.scalytica.symbiotic.mongodb.bson

import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import net.scalytica.symbiotic.api.types.PersistentType.{
  UserStamp,
  VersionStamp
}
import net.scalytica.symbiotic.api.types.TransUserId
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
    implicit def userstamp_toBSON(x: UserStamp): DBObject =
      MongoDBObject(
        "date" -> x.date.toDate,
        "by"   -> x.by.value
      )

    implicit def userstamp_fromBSON(
        dbo: DBObject
    )(implicit f: TransUserId): UserStamp =
      UserStamp(
        date = dbo.as[java.util.Date]("date"),
        by = f(dbo.as[String]("by"))
      )
  }

  trait VersionStampBSONConverter extends UserStampBSONConverter {

    implicit def versionstamp_toBSON(x: VersionStamp): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "version" -> x.version
      x.created.foreach(b += "created"   -> userstamp_toBSON(_))
      x.modified.foreach(b += "modified" -> userstamp_toBSON(_))

      b.result()
    }

    implicit def versionstamp_fromBSON(
        dbo: DBObject
    )(implicit f: TransUserId): VersionStamp =
      VersionStamp(
        version = dbo.getAsOrElse[Int]("version", 0),
        created = dbo.getAs[DBObject]("created").map(userstamp_fromBSON),
        modified = dbo.getAs[DBObject]("modified").map(userstamp_fromBSON)
      )
  }

}
