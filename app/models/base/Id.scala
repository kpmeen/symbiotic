/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import com.mongodb.casbah.TypeImports.ObjectId

/**
 * Base trait defining an Id throughout the system. All type specific Id's should extend this trait
 */
trait Id {
  val id: ObjectId

  def asString = id.toString
}





