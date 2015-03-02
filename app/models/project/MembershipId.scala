/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import models.base.{Id, WithIdTransformers}
import org.bson.types.ObjectId

/**
 * Id type for project membership
 */
case class MembershipId(id: ObjectId) extends Id

object MembershipId extends WithIdTransformers {

  implicit val membershipIdReads = reads[MembershipId](MembershipId.apply)
  implicit val membershipIdWrites = writes[MembershipId]

  def fromString(mid: String): Option[MembershipId] = Option(new ObjectId(mid)).flatMap(oid => Option(MembershipId(oid)))

}