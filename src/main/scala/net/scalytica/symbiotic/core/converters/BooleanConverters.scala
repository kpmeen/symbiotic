package net.scalytica.symbiotic.core.converters

object BooleanConverters {
  implicit class RichBoolean(val bool: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (bool) Some(a) else None
  }
}
