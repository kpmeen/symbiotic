/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import core.converters.WithIdConverters
import models.base.Id

/**
 * An abstract trait defining a Party (person or company)
 */
abstract class Party[T <: Id] {
  val id: Option[T]
}

/**
 * Use this to implement an organisational party (company)
 */
trait Organization extends Party[OrganizationId]

/**
 * Use this to implement a party that represents a person.
 */
trait Individual extends Party[UserId]

/**
 * Id implementation for UserId.
 */
case class UserId(id: String) extends Id

object UserId extends WithIdConverters[UserId] {
  implicit val userIdReads = reads(UserId.apply)
  implicit val userIdWrites = writes

  override implicit def asId(s: String): UserId = UserId(s)
}

/**
 * Id implementation for OrganizationId.
 */
case class OrganizationId(id: String) extends Id

object OrganizationId extends WithIdConverters[OrganizationId] {
  implicit val orgIdReads = reads(OrganizationId.apply)
  implicit val orgIdWrites = writes

  override implicit def asId(s: String): OrganizationId = OrganizationId(s)
}

