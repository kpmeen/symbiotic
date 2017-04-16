package net.scalytica.symbiotic.api.types

import java.util.UUID

/**
 * Base trait defining an Id throughout the system. All type specific Id's
 * should extend this trait
 */
abstract class Id {

  val value: String

  validate()

  @throws(classOf[IllegalArgumentException])
  private def validate() = {
    if (value.nonEmpty)
      Option(UUID.fromString(value)).getOrElse {
        throw new IllegalArgumentException("Id must be a valid UUID.")
      }
  }

}
