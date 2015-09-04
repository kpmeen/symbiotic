/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import converters.IdConverters
import models.base.Id

/**
 * Id type for project membership
 */
case class MembershipId(value: String) extends Id

object MembershipId extends IdConverters[MembershipId] {

  implicit val membershipIdReads = reads(MembershipId.apply)
  implicit val membershipIdWrites = writes

  override implicit def asId(s: String): MembershipId = MembershipId(s)

}