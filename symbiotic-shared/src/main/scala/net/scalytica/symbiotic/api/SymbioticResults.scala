package net.scalytica.symbiotic.api

import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.Path

object SymbioticResults {

  // Aliases to help API readability
  type SaveResult[T]   = SymRes[T]
  type LockResult[T]   = SymRes[T]
  type UnlockResult[T] = SymRes[T]
  type DeleteResult[T] = SymRes[T]
  type GetResult[T]    = SymRes[T]
  type MoveResult[T]   = SymRes[T]

  sealed trait SymRes[+A] {
    self =>

    @throws(classOf[IllegalArgumentException])
    protected def failedValueAccess() =
      throw new NoSuchElementException(s"Can't get value from ${self.getClass}")

    def get: A

    def success: Boolean

    final def failed: Boolean = !success

    def map[B](f: A => B): SymRes[B] = this match {
      case Ok(v)  => Ok(f(v))
      case ko: Ko => ko
    }

    def foreach[U](f: A => U): Unit = this match {
      case Ok(v) => f(v)
      case _     => ()
    }

    def flatMap[B](f: A => SymRes[B]): SymRes[B] =
      this match {
        case Ok(v)  => f(v)
        case ko: Ko => ko
      }

    def flatten[B](implicit ev: A <:< SymRes[B]): SymRes[B] =
      this match {
        case Ok(v)  => v
        case ko: Ko => ko
      }

    final def getOrElse[B >: A](default: => B): B = if (failed) default else get

    final def toOption: Option[A] = if (failed) None else Some(get)

  }

  case class Ok[A](value: A) extends SymRes[A] {
    override val success = true

    override def get = value
  }

  sealed trait Ko extends SymRes[Nothing] {
    override val success = false

    override def get = failedValueAccess()
  }

  case class NotFound() extends Ko

  case class NotModified() extends Ko

  case class NotLocked() extends Ko

  case class NotEditable(msg: String) extends Ko

  case class IllegalDestination(msg: String, mp: Option[Path] = None) extends Ko

  object IllegalDestination {
    def apply(msg: String, p: Path): IllegalDestination =
      IllegalDestination(msg, Some(p))
  }

  case class InvalidData(msg: String) extends Ko

  case class ResourceLocked(msg: String, mby: Option[PartyId] = None) extends Ko

  object ResourceLocked {
    def apply(msg: String, by: PartyId): ResourceLocked =
      ResourceLocked(msg, Some(by))
  }

  case class Failed(msg: String) extends Ko

}
