/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import play.api.libs.json.Json

/**
 * Represents the full name of an individual (a.k.a. person).
 */
case class Name(first: Option[String], middle: Option[String], last: Option[String])


object Name {
  implicit val nameFormat = Json.format[Name]

  def toBSON(n: Name): DBObject = {
    val builder = MongoDBObject.newBuilder
    n.first.foreach(f => builder += "first" -> f)
    n.middle.foreach(m => builder += "middle" -> m)
    n.last.foreach(l => builder += "last" -> l)

    builder.result()
  }

  def fromBSON(dbo: DBObject): Name = {
    Name(
      first = dbo.getAs[String]("first"),
      middle = dbo.getAs[String]("middle"),
      last = dbo.getAs[String]("last")
    )
  }
}
