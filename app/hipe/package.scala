import hipe.core.{StepId, StepGroupId, ProcessId}
import play.api.libs.json._

/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package object hipe {

  /**
   * Contains command definitions for the below move functions
   *
   * TODO: Refactor to package object
   */
  object MoveStepCommands {

    private[this] val CommandAttr = "command"
    private[this] val InGroupCmd = "InGroup"
    private[this] val ToGroupCmd = "ToGroup"
    private[this] val ToNewGroupCmd = "ToNewGroup"

    sealed trait MoveStep {
      val commandName: String
    }

    case class InGroup(pid: ProcessId, sgid: StepGroupId, sid: StepId, to: Int) extends MoveStep {
      override val commandName: String = InGroupCmd
    }

    case class ToGroup(pid: ProcessId, sid: StepId, dest: StepGroupId, pos: Int) extends MoveStep {
      override val commandName: String = ToGroupCmd
    }

    case class ToNewGroup(pid: ProcessId, sid: StepId, to: Int) extends MoveStep {
      override val commandName: String = ToNewGroupCmd
    }

    private[this] val ingrpFormat: Format[InGroup] = Json.format[InGroup]
    private[this] val togrpFormat: Format[ToGroup] = Json.format[ToGroup]
    private[this] val tngrpFormat: Format[ToNewGroup] = Json.format[ToNewGroup]

    implicit val reads: Reads[MoveStep] = Reads { jsv =>
      (jsv \ CommandAttr).as[String] match {
        case `InGroupCmd` => JsSuccess(jsv.as(ingrpFormat))
        case `ToGroupCmd` => JsSuccess(jsv.as(togrpFormat))
        case `ToNewGroupCmd` => JsSuccess(jsv.as(tngrpFormat))
        case err => JsError(s"Not a supported MoveStep command: $err")
      }
    }

    implicit val writes: Writes[MoveStep] = Writes {
      case ing: InGroup => ingrpFormat.writes(ing).as[JsObject] ++ Json.obj(CommandAttr -> InGroupCmd)
      case tog: ToGroup => togrpFormat.writes(tog).as[JsObject] ++ Json.obj(CommandAttr -> ToGroupCmd)
      case tng: ToNewGroup => tngrpFormat.writes(tng).as[JsObject] ++ Json.obj(CommandAttr -> ToNewGroupCmd)
    }

  }

}
