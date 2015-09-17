/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import com.mongodb.casbah.Imports._
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.PersistentType.VersionStamp
import models.base.{PersistentTypeConverters, ShortName}
import models.party.PartyBaseTypes.{OrgId, Party}
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try

/**
 * Representation of a Company/Organization in the system
 */
case class Organisation(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  id: Option[OrgId] = None,
  shortName: ShortName,
  name: String,
  description: Option[String] = None,
  hasLogo: Boolean = false) extends Party

object Organisation extends PersistentTypeConverters with DateTimeConverters with DefaultDB with WithMongoIndex with ObjectBSONConverters[Organisation] {

  val logger = LoggerFactory.getLogger(classOf[Organisation])

  implicit val f: Format[Organisation] = Json.format[Organisation]

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("shortName", unique = true),
    Indexable("name", unique = false)
  ), collection)

  override val collectionName: String = "organisations"

  ensureIndex()

  implicit override def fromBSON(dbo: DBObject): Organisation =
    Organisation(
      _id = dbo.getAs[ObjectId]("_id"),
      v = dbo.getAs[DBObject]("v").map(VersionStamp.fromBSON),
      id = dbo.getAs[String]("id"),
      shortName = ShortName(dbo.as[String]("shortName")),
      name = dbo.as[String]("name"),
      description = dbo.getAs[String]("description"),
      hasLogo = dbo.getAs[Boolean]("hasLogo").getOrElse(false)
    )

  implicit override def toBSON(org: Organisation): DBObject = {
    val builder = MongoDBObject.newBuilder
    org._id.foreach(builder += "_id" -> _)
    org.v.foreach(builder += "v" -> VersionStamp.toBSON(_))
    org.id.foreach(builder += "id" -> _.value)
    builder += "shortName" -> org.shortName.code
    builder += "name" -> org.name
    org.description.foreach(builder += "description" -> _)
    builder += "hasLogo" -> org.hasLogo

    builder.result()
  }

  /**
   *
   * @param org
   */
  def save(org: Organisation): Unit = {
    Try {
      val res = collection.save(org)

      if (res.isUpdateOfExisting) logger.info("Updated existing organisation")
      else logger.info("Inserted new organisation")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"Organisation could not be saved", t)
    }
  }

  /**
   *
   * @param oid
   * @return
   */
  def findById(oid: OrgId): Option[Organisation] =
    collection.findOne(MongoDBObject("id" -> oid.value)).map(oct => fromBSON(oct))

  /**
   *
   * @param sname
   * @return
   */
  def findByShortName(sname: ShortName): Option[Organisation] =
    collection.findOne(MongoDBObject("shortName" -> sname.code)).map(oct => fromBSON(oct))
}