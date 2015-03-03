/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.converters

import com.mongodb.DBObject

trait WithBSONConverters[T] {

  implicit def toBSON(x: T): DBObject

  implicit def fromBSON(dbo: DBObject): T

}
