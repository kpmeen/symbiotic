/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import core.converters.WithIdConverters
import models.base.Id
import org.bson.types.ObjectId

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
 * Abstract id for
 */
case class OrganizationId(id: ObjectId) extends Id

object OrganizationId extends WithIdConverters[OrganizationId] {
  implicit val orgIdReads = reads(OrganizationId.apply)
  implicit val orgIdWrites = writes

  override implicit def asId(oid: ObjectId): OrganizationId = OrganizationId(oid)

  override implicit def asId(s: String): OrganizationId = OrganizationId(new ObjectId(s))
}

