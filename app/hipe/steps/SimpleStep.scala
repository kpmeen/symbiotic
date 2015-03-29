/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.steps

import hipe.{Step, StepId}
import play.api.libs.json.{Json, Reads, Writes}

/**
 * Represents a simple implementation of Step as applied in a Process definition
 *
 * @see Step
 */
case class SimpleStep(id: StepId, name: String, description: Option[String]) extends Step

object SimpleStep {
  implicit val r: Reads[SimpleStep] = Json.reads[SimpleStep]
  implicit val w: Writes[SimpleStep] = Json.writes[SimpleStep]
}