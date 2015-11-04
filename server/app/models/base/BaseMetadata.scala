/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import com.mongodb.casbah.Imports._

trait BaseMetadata

trait BaseMetadataConverter[A <: BaseMetadata] {
  implicit def toBSON(fmd: A): DBObject

  implicit def fromBSON(dbo: DBObject): A
}
