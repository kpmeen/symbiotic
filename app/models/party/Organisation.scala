/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import models.base.PersistentType.VersionStamp
import models.base.{PersistentTypeConverters, ShortName}
import models.party.PartyBaseTypes.{OrgId, Party}
import org.bson.types.ObjectId
import play.api.libs.json._

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
  hasLogo: Option[Boolean] = None) extends Party

object Organisation extends PersistentTypeConverters {

  implicit val f: Format[Organisation] = Json.format[Organisation]

}