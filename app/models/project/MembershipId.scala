/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.WithIdConverters
import models.base.Id
import org.bson.types.ObjectId

/**
 * Id type for project membership
 */
case class MembershipId(id: ObjectId) extends Id

object MembershipId extends WithIdConverters[MembershipId] {

  implicit val membershipIdReads = reads(MembershipId.apply)
  implicit val membershipIdWrites = writes

  override implicit def asId(oid: ObjectId): MembershipId = MembershipId(oid)

  def fromString(mid: String): Option[MembershipId] = Option(new ObjectId(mid)).flatMap(oid => Option(MembershipId(oid)))

}