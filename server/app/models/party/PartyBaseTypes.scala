/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import core.converters.IdConverters
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, Id}
import play.api.libs.json.Format

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
  case class UserId(value: String) extends PartyId

  object UserId extends IdConverters[UserId] {
    implicit val f: Format[UserId] = Format(reads(UserId.apply), writes)

    override implicit def asId(s: String): UserId = UserId(s)
  }

}