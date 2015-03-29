/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.steps.SimpleStep
import play.api.libs.json._

trait ProcessStepOperations

/**
 * Defines the basic elements of a Step in a process. This is the starting point for creating more
 * elaborate building blocks that can be composed together as a Process.
 */
trait Step {
  val id: StepId
  val name: String
  val description: Option[String]
}

/**
 * It is necessary to implement a type discriminator for Step implementations ($tpe="$type"). This is due to
 * de-serialization. If not present, it would be almost impossible to know which child type is actually represented in the
 * serialized format when de-serializing from the parent type.
 *
 * IMPORTANT: Ensure that the value is a String with the value = "NameOfImplClass"
 *
 * Given a custom case class like this:
 * {{{
 *   case class FooStep(id: StepId, name: String, description: Option[String], bar: Int) extends Step
 *
 *   object FooStep {
 *     implicit val fooReads: Reads[FooStep] = Json.reads[FooStep]
 *     implicit val fooWrites: Writes[FooWrites] = Json.writes[FooStep]
 *   }
 * }}}
 *
 * It's necessary to add the following code to the Step companion:
 * {{{
 *   ...
 *   val fooStepClassName = classOf[FooStep].getSimpleName
 *   ...
 *
 *   implicit val reads: Reads[Step] = Reads { jsv =>
 *     ...
 *       case `fooStepClassName` => JsSuccess(jsv.as[FooStep](FooStep.fooReads))
 *     ...
 *   }
 *
 *   implicit val writes: Writes[Step] = Writes {
 *     case fs: FooStep => FooStep.fooWrites.writes(fs).as[JsObject] ++ Json.obj($tpe -> fooStepClassName)
 *   }
 * }}}
 *
 * Another important detail is that the implementation type _must_ be added to the reads/writes in the companion
 * object of Step.
 *
 * @return returns the String representing the implementation of the specific Step. Make sure it's unique.
 */
object Step {
  val $tpe = "$type"
  val simpleStepClassName = classOf[SimpleStep].getSimpleName

  implicit val reads: Reads[Step] = Reads { jsv =>
    (jsv \ $tpe).as[String] match {
      case `simpleStepClassName` => JsSuccess(jsv.as[SimpleStep](SimpleStep.r))
    }
  }

  implicit val writes: Writes[Step] = Writes {
    case simpleStep: SimpleStep => SimpleStep.w.writes(simpleStep).as[JsObject] ++ Json.obj($tpe -> simpleStepClassName)
  }

  implicit val format: Format[Step] = Format(reads, writes)
}

/**
 * Types indicating which steps are surrounding the current Step.
 */
private[hipe] sealed trait PrevNextStepType

private[hipe] case class PrevNextStep(prev: StepId, next: StepId) extends PrevNextStepType

private[hipe] case class PrevOnlyStep(prev: StepId) extends PrevNextStepType

private[hipe] case class NextOnlyStep(next: StepId) extends PrevNextStepType