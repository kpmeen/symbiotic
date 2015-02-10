/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core

import org.bson.types.ObjectId

/**
 * Id implementation for UserId.
 */
case class UserId(id: ObjectId) extends Id

object UserId extends WithIdTransformers {
  implicit val userIdReads = reads[UserId](UserId.apply)
  implicit val userIdWrites = writes[UserId]

  def fromString(uid: String): Option[UserId] = Option(new ObjectId(uid)).flatMap(oid => Option(UserId(oid)))
}