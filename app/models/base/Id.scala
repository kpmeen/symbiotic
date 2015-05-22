/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

/**
 * Base trait defining an Id throughout the system. All type specific Id's should extend this trait
 */
trait Id {

  val value: String
}



