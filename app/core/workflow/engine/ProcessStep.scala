/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.workflow.engine

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Defines the basic traits for a Step in a process. This is the starting point of creating more
 * elaborate building blocks that can be composed together as a Process.
 */
sealed trait ProcessStep {
  val id: StepId
  val name: String
  val description: Option[String]

  /**
   * It is necessary to implement a type discriminator for ProcessStep implementations. This is due to de-serialization
   * from JSON and other formats. If not present, it would be almost impossible to know which type is actually
   * represented in the JSON when de-serializing from the parent.
   *
   * IMPORTANT: Ensure that the value is a String with the value = "NameOfImplClass"
   *
   * Example usage
   * {{{
   *   case class FooStep(id: StepId, name: String, description: Option[String], bar: Int) extends ProcessStep {
   *     override val $stepType = "FooStep"
   *   }
   * }}}
   *
   * Another important details is that the implementation type _must_ be added to the reads/writes in the companion
   * object of ProcessStep.
   *
   * @return returns the String representing the implementation of the specific ProcessStep. Make sure it's unique.
   */
  def $stepType: String
}

object ProcessStep {

  implicit val reads: Reads[ProcessStep] = (
    (__ \ "$stepType").read[String] and
      __.json.pick
    ).tupled.flatMap[ProcessStep] {
    case (stype, js) => stype match {
      case "SimpleStep" => Reads { _ => Json.fromJson[SimpleStep](js)(Json.reads[SimpleStep])}.map(c => c: ProcessStep)
      case _ => Reads { s => JsError(s"Did not recognize value for stepType = $s")}
    }
  }

  implicit val writes: Writes[ProcessStep] = Writes {
    (p: ProcessStep) =>
      val jsv = p match {
        case ss: SimpleStep => w[SimpleStep](ss).as[JsObject]
      }
      jsv ++ Json.obj("$stepType" -> p.$stepType)
  }

  private def w[A <: ProcessStep](o: A): JsValue = {
    o match {
      case ss: SimpleStep => Json.writes[SimpleStep].writes(ss)
    }
  }

}

/**
 * Represents a simple implementation of Step as applied in a Process definition
 *
 * @see ProcessStep
 */
case class SimpleStep(id: StepId, name: String, description: Option[String]) extends ProcessStep {

  override val $stepType: String = "SimpleStep"

}