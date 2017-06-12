package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.joda.time.DateTime

/**
 * Used for handling (un)locking files for change (or version incrementation)
 */
case class Lock(by: UserId, date: DateTime)

object Lock {

  object LockOpStatusTypes {

    sealed trait LockOpStatus[+A]

    case class LockApplied[A](res: A) extends LockOpStatus[A]

    case class LockRemoved[A](res: A) extends LockOpStatus[A]

    case class Locked(res: UserId) extends LockOpStatus[Nothing]

    case class NotAllowed() extends LockOpStatus[Nothing]

    case class NotLocked() extends LockOpStatus[Nothing]

    case class LockError(reason: String) extends LockOpStatus[Nothing]

  }

}
