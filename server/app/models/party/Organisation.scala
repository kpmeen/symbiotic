/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import core.converters.DateTimeConverters
import models.base.PersistentType.VersionStamp
import models.base.{PersistentTypeConverters, ShortName}
import models.party.PartyBaseTypes.{OrganisationId, Party}
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import play.api.libs.json._

/**
 * Representation of a Company/Organization in the system
 */
case class Organisation(
  _id: Option[ObjectId] = None,
  v: Option[VersionStamp] = None,
  id: Option[OrganisationId] = None,
  shortName: ShortName,
  name: String,
  description: Option[String] = None,
  hasLogo: Boolean = false
) extends Party

object Organisation extends PersistentTypeConverters with DateTimeConverters {

  val logger = LoggerFactory.getLogger(classOf[Organisation])

  implicit val f: Format[Organisation] = Json.format[Organisation]

}