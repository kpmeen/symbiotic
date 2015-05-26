/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import play.api.libs.json._

import scala.reflect._

object States {

  /*
    TODO: May need some additional state types...
   */

  sealed trait State

  sealed trait TaskState extends State

  sealed trait AssignmentState extends State

  object TaskStates {

    case class New() extends TaskState

    case class Ready() extends TaskState

    case class Approved() extends TaskState

    case class Consolidated() extends TaskState

    case class Rejected() extends TaskState

    case class Closed() extends TaskState

  }

  object AssignmentStates {

    case class Open() extends AssignmentState

    case class Completed() extends AssignmentState

    case class Aborted() extends AssignmentState

  }

  private object StrValues {
    val New = TaskState.typeAsString[TaskStates.New]
    val Ready = TaskState.typeAsString[TaskStates.Ready]
    val Approved = TaskState.typeAsString[TaskStates.Approved]
    val Consolidated = TaskState.typeAsString[TaskStates.Consolidated]
    val Rejected = TaskState.typeAsString[TaskStates.Rejected]
    val Closed = TaskState.typeAsString[TaskStates.Closed]

    val Open = AssignmentState.typeAsString[AssignmentStates.Open]
    val Completed = AssignmentState.typeAsString[AssignmentStates.Completed]
    val Aborted = AssignmentState.typeAsString[AssignmentStates.Aborted]
  }

  trait StateConverters[A <: State] {
    def asState(arg: String): A

    implicit def typeAsString[T <: State](implicit ct: ClassTag[T]): String = classTag[T].runtimeClass.getSimpleName

    implicit def asString[T <: State](arg: T)(implicit ct: ClassTag[T]): String = arg.getClass.getSimpleName

    implicit def optAsString[T <: State](arg: Option[T] = None)(implicit ct: ClassTag[T]): String =
      arg.fold(typeAsString(ct))(asString)

  }

  object TaskState extends StateConverters[TaskState] {

    implicit val reads: Reads[TaskState] = __.read[String].map(o => asState(o))
    implicit val writes: Writes[TaskState] = Writes(state => JsString(asString(state)))

    implicit def asState(arg: String): TaskState = {
      arg match {
        case StrValues.New => TaskStates.New()
        case StrValues.Ready => TaskStates.Ready()
        case StrValues.Approved => TaskStates.Approved()
        case StrValues.Consolidated => TaskStates.Consolidated()
        case StrValues.Rejected => TaskStates.Rejected()
        case StrValues.Closed => TaskStates.Closed()
      }
    }
  }

  object AssignmentState extends StateConverters[AssignmentState] {

    implicit val reads: Reads[AssignmentState] = __.read[String].map(o => asState(o))
    implicit val writes: Writes[AssignmentState] = Writes(state => JsString(asString(state)))

    implicit def asState(arg: String): AssignmentState = {
      arg match {
        case StrValues.Open => AssignmentStates.Open()
        case StrValues.Completed => AssignmentStates.Completed()
        case StrValues.Aborted => AssignmentStates.Aborted()
      }
    }
  }

  // ... what else?
}

