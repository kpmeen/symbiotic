/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import core.converters.IdConverters
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, Id}
import play.api.libs.json.Format
import core.security.authorisation.Principal

object PartyBaseTypes {

  /**
   * An abstraction defining a Party (person or organsation)
   */
  abstract class Party extends PersistentType {
    val id: Option[PartyId]
    val v: Option[VersionStamp]
  }

  sealed trait PartyId extends Id

  /**
   * Id implementation to be used for identifying Users
   */
  case class UserId(value: String) extends PartyId with Principal

  object UserId extends IdConverters[UserId] {
    implicit val f: Format[UserId] = Format(reads(UserId.apply), writes)

    override implicit def asId(s: String): UserId = UserId(s)
  }

  /**
   * Id implementation to be used for identifying Organisations.
   */
  case class OrgId(value: String) extends PartyId

  object OrgId extends IdConverters[OrgId] {
    implicit val f: Format[OrgId] = Format(reads(OrgId.apply), writes)

    override implicit def asId(s: String): OrgId = OrgId(s)
  }

}