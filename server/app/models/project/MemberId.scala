/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.IdConverters
import models.base.Id

/**
 * Id type for project membership
 */
case class MemberId(value: String) extends Id

object MemberId extends IdConverters[MemberId] {

  implicit val membershipIdReads = reads(MemberId.apply)
  implicit val membershipIdWrites = writes

  override implicit def asId(s: String): MemberId = MemberId(s)

}