package net.scalytica.symbiotic.test.utils

import net.scalytica.symbiotic.api.SymbioticResults.SymRes
import org.scalactic._
import org.scalatest.exceptions.{StackDepthException, TestFailedException}

trait SymResValues {

  implicit def convertSymResToValuable[T](
      sr: SymRes[T]
  )(implicit pos: source.Position): Valuable[T] = new Valuable(sr, pos)

  class Valuable[T](sr: SymRes[T], pos: source.Position) {

    def value: T =
      try {
        sr.get
      } catch {
        case cause: NoSuchElementException =>
          throw new TestFailedException(
            (_: StackDepthException) => Some("SymRes value was not defined"),
            Some(cause),
            pos
          )
      }
  }

}

object SymResValues extends SymResValues
