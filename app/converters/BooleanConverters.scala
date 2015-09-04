/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package converters

object BooleanConverters {

  implicit class RichBoolean(val bool: Boolean) extends AnyVal {
    final def either[A, B](a: => A, b: => B): Either[B, A] = if (bool) Right(a) else Left(b)

    final def option[A](a: => A): Option[A] = if (bool) Some(a) else None
  }

}