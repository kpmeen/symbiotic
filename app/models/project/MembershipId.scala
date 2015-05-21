/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.WithDBIdConverters
import models.base.DBId

/**
 * Id type for project membership
 */
case class MembershipId(value: String) extends DBId

object MembershipId extends WithDBIdConverters[MembershipId] {

  implicit val membershipIdReads = reads(MembershipId.apply)
  implicit val membershipIdWrites = writes

  override implicit def asId(s: String): MembershipId = MembershipId(s)

}