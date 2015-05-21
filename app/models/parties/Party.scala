/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import core.converters.WithDBIdConverters
import models.base.DBId

/**
 * An abstract trait defining a Party (person or company)
 */
abstract class Party[T <: DBId] {
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
case class UserId(value: String) extends DBId

object UserId extends WithDBIdConverters[UserId] {
  implicit val userIdReads = reads(UserId.apply)
  implicit val userIdWrites = writes

  override implicit def asId(s: String): UserId = UserId(s)
}

/**
 * Id implementation for OrganizationId.
 */
case class OrganizationId(value: String) extends DBId

object OrganizationId extends WithDBIdConverters[OrganizationId] {
  implicit val orgIdReads = reads(OrganizationId.apply)
  implicit val orgIdWrites = writes

  override implicit def asId(s: String): OrganizationId = OrganizationId(s)
}

