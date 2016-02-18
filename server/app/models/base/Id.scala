/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import java.util.UUID

import scala.util.{Failure, Success, Try}

/**
 * Base trait defining an Id throughout the system. All type specific Id's should extend this trait
 */
abstract class Id {

  val value: String

  assertId()

  def assertId() = assert(
    assertion = Try(UUID.fromString(value)) match {
    case Success(s) => true
    case Failure(e) => false
  },
    message = "Value is not a valid format"
  )
}

