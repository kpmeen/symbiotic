package net.scalytica.symbiotic.api.types

/**
 * Base trait defining an Id throughout the system. All type specific Id's
 * should extend this trait
 */
abstract class Id {
  val value: String
}
