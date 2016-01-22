/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import core.converters.DateTimeConverters
import models.party.PartyBaseTypes.UserId
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

/**
 * Used for handling (un)locking files for change (or version incrementation)
 */
case class Lock(by: UserId, date: DateTime)

object Lock extends DateTimeConverters {
  implicit val lockFormat: Format[Lock] = Json.format[Lock]

  object LockOpStatusTypes {

    sealed trait LockOpStatus[A]

    case class LockApplied[A](res: A) extends LockOpStatus[A]

    case class Locked[A](res: UserId) extends LockOpStatus[A]

    case class NotAllowed[A]() extends LockOpStatus[A]

    case class NotLocked[A]() extends LockOpStatus[A]

    case class LockError[A](reason: String) extends LockOpStatus[A]

  }

}