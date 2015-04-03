/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.steps

import com.mongodb.casbah.commons.Imports._
import hipe.core.{StepConverters, Step, StepId}
import play.api.libs.json.{Json, Reads, Writes}

import scala.reflect.ClassTag

/**
 * Represents a simple implementation of Step as applied in a Process definition
 *
 * @see Step
 */
case class SimpleStep(id: StepId, name: String, description: Option[String]) extends Step

object SimpleStep extends StepConverters[SimpleStep] {
  implicit val r: Reads[SimpleStep] = Json.reads[SimpleStep]
  implicit val w: Writes[SimpleStep] = Json.writes[SimpleStep]

  override def toBSON(s: SimpleStep)(implicit ct: ClassTag[SimpleStep]): MongoDBObject = {
    ???
  }

  override def fromBSON(dbo: MongoDBObject)(implicit ct: ClassTag[SimpleStep]): SimpleStep = {
    ???
  }
}