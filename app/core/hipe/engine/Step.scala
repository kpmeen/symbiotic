/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.hipe.engine

import play.api.libs.json._

trait ProcessStepOperations

/**
 * Defines the basic elements of a Step in a process. This is the starting point for creating more
 * elaborate building blocks that can be composed together as a Process.
 */
sealed trait Step {
  val id: StepId
  val name: String
  val description: Option[String]
}

/**
 * Represents a simple implementation of Step as applied in a Process definition
 *
 * @see ProcessStep
 */
case class SimpleStep(id: StepId, name: String, description: Option[String]) extends Step {

  implicit val reads: Format[SimpleStep] = Json.format[SimpleStep]

}

/**
 * Types indicating which steps are surrounding the current Step.
 */
private[engine] sealed trait PrevNextStepType

private[engine] case class PrevNextStep(prev: StepId, next: StepId) extends PrevNextStepType

private[engine] case class PrevOnlyStep(prev: StepId) extends PrevNextStepType

private[engine] case class NextOnlyStep(next: StepId) extends PrevNextStepType