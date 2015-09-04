/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package converters

import com.mongodb.casbah.commons.Imports._

sealed trait BaseBSONConverters[T, TO, FROM] {

  def toBSON(x: T): TO

  def fromBSON(dbo: FROM): T

}

trait ObjectBSONConverters[T] extends BaseBSONConverters[T, DBObject, DBObject]

trait ListBSONConverters[T] extends BaseBSONConverters[T, Seq[DBObject], MongoDBList]