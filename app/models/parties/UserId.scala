/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.parties

import core.converters.WithIdConverters
import models.base.Id
import org.bson.types.ObjectId

/**
 * Id implementation for UserId.
 */
case class UserId(id: ObjectId) extends Id

object UserId extends WithIdConverters[UserId] {
  implicit val userIdReads = reads(UserId.apply)
  implicit val userIdWrites = writes

  override implicit def asId(oid: ObjectId): UserId = UserId(oid)

  override implicit def asId(s: String): UserId = UserId(new ObjectId(s))
}