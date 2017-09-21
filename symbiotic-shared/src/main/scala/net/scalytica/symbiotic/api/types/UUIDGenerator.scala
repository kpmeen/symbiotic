package net.scalytica.symbiotic.api.types

import java.util.UUID

import scala.util.Try

object UUIDGenerator {

  def generate(): UUID = UUID.randomUUID()

  def generateOpt(): Option[UUID] = Try(UUID.randomUUID()).toOption

  def fromString(s: String): Option[UUID] =
    Try {
      if (s.length != 36)
        throw new IllegalArgumentException(s"$s is not a valid UUID.")

      UUID.fromString(s)
    }.toOption

}
