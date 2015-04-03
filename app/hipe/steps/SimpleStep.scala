/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.steps

import com.mongodb.casbah.commons.Imports._
import hipe.core.{Step, StepConverters, StepId}
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
    val builder = MongoDBObject.newBuilder
    builder += "id" -> s.id.asOID
    builder += "name" -> s.name
    s.description.foreach(d => builder += "description" -> d)

    builder.result()
  }

  override def fromBSON(dbo: MongoDBObject)(implicit ct: ClassTag[SimpleStep]): SimpleStep = {
    SimpleStep(
      id = StepId.asId(dbo.as[ObjectId]("id")),
      name = dbo.as[String]("name"),
      description = dbo.getAs[String]("description")
    )
  }
}