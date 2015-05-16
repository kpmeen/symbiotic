/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.converters

import com.mongodb.casbah.commons.Imports._

sealed trait BaseBSONConverters[T, M] {

  def toBSON(x: T): M

  def fromBSON(dbo: M): T

}

trait WithObjectBSONConverters[T] extends BaseBSONConverters[T, DBObject]

trait WithListBSONConverters[T] extends BaseBSONConverters[T, MongoDBList]