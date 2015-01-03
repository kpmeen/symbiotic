/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.core

import reactivemongo.bson.BSONObjectID

trait Id {
  val id: BSONObjectID
}
