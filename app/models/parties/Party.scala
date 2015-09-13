/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import converters.IdConverters
import models.base.PersistentType.VersionStamp
import models.base.{Id, PersistentType}
import play.api.libs.json.Format

/**
 * An abstract trait defining a Party (person or company)
 */
abstract class Party[T <: Id] extends PersistentType {
  val id: Option[T]
  val v: Option[VersionStamp]
}

/**
 * Use this to implement an organisational party (company)
 */
trait Organization extends Party[CompanyId]

/**
 * Use this to implement a party that represents a person.
 */
trait Individual extends Party[UserId]


sealed trait PartyId extends Id

/**
 * Id implementation for UserId.
 */
case class UserId(value: String) extends PartyId

object UserId extends IdConverters[UserId] {
  implicit val f: Format[UserId] = Format(reads(UserId.apply), writes)

  override implicit def asId(s: String): UserId = UserId(s)
}

/**
 * Id implementation for OrganizationId.
 */
case class CompanyId(value: String) extends PartyId

object CompanyId extends IdConverters[CompanyId] {
  implicit val f: Format[CompanyId] = Format(reads(CompanyId.apply), writes)

  override implicit def asId(s: String): CompanyId = CompanyId(s)
}

