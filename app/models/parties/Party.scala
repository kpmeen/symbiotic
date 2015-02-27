/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import models.core.{Id, WithIdTransformers}
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

object OrganizationId extends WithIdTransformers {
  implicit val orgIdReads = reads[OrganizationId](OrganizationId.apply)
  implicit val orgIdWrites = writes[OrganizationId]
}

/**
 * Id implementation for UserId.
 */
case class UserId(id: ObjectId) extends Id

object UserId extends WithIdTransformers {
  implicit val userIdReads = reads[UserId](UserId.apply)
  implicit val userIdWrites = writes[UserId]

  def fromString(uid: String): Option[UserId] = Option(new ObjectId(uid)).flatMap(oid => Option(UserId(oid)))
}