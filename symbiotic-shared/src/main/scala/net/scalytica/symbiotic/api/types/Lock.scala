package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.joda.time.DateTime

/**
 * Used for handling (un)locking files for change (or version incrementation)
 */
case class Lock(by: UserId, date: DateTime)

object Lock {

  object LockOpStatusTypes {

    sealed trait LockOpStatus[A]

    case class LockApplied[A](res: A) extends LockOpStatus[A]

    case class Locked[A](res: UserId) extends LockOpStatus[A]

    case class NotAllowed[A]() extends LockOpStatus[A]

    case class NotLocked[A]() extends LockOpStatus[A]

    case class LockError[A](reason: String) extends LockOpStatus[A]

  }

}
