/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.parties

import core.converters.WithIdConverters
import models.base.Id

/**
 * Id implementation for UserId.
 */
case class UserId(id: String) extends Id

object UserId extends WithIdConverters[UserId] {
  implicit val userIdReads = reads(UserId.apply)
  implicit val userIdWrites = writes

  override implicit def asId(s: String): UserId = UserId(s)
}