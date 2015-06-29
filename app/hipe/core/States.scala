/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import play.api.libs.json._

import scala.reflect._

object States {

  sealed trait State

  sealed trait TaskState extends State

  sealed trait AssignmentState extends State

  object TaskStates {

    case class Open() extends TaskState

    case class InProgress() extends TaskState

    case class Approved() extends TaskState

    case class Consolidated() extends TaskState

    case class NotApproved() extends TaskState

    case class Rejected() extends TaskState

    case class Closed() extends TaskState

  }

  object AssignmentStates {

    case class Available() extends AssignmentState

    case class Assigned() extends AssignmentState

    case class Completed() extends AssignmentState

    case class Aborted() extends AssignmentState

  }

  private object StrValues {

    import TaskStates._

    val Open = TaskState.typeAsString[Open]
    val InProgress = TaskState.typeAsString[InProgress]
    val Approved = TaskState.typeAsString[Approved]
    val Consolidated = TaskState.typeAsString[Consolidated]
    val NotApproved = TaskState.typeAsString[NotApproved]
    val Rejected = TaskState.typeAsString[Rejected]
    val Closed = TaskState.typeAsString[Closed]

    import AssignmentStates._

    val Available = AssignmentState.typeAsString[Available]
    val Assigned = AssignmentState.typeAsString[Assigned]
    val Completed = AssignmentState.typeAsString[Completed]
    val Aborted = AssignmentState.typeAsString[Aborted]
  }

  trait StateConverters[A <: State] {
    def asState(arg: String): A

    private[this] def simpleClassName(c: Class[_]) = {
      val n = c.getName
      n.drop(n.lastIndexOf("$")).drop(1)
    }

    implicit def typeAsString[T <: A](implicit ct: ClassTag[T]): String = simpleClassName(ct.runtimeClass)

    implicit def asString[T <: A](arg: T): String = simpleClassName(arg.getClass)

    implicit def optAsString[T <: A](arg: Option[T] = None)(implicit ct: ClassTag[T]): String =
      arg.fold(typeAsString(ct))(asString)

  }

  object TaskState extends StateConverters[TaskState] {

    import TaskStates._

    implicit val reads: Reads[TaskState] = __.read[String].map(o => asState(o))
    implicit val writes: Writes[TaskState] = Writes(state => JsString(asString(state)))

    override implicit def asState(arg: String): TaskState = {
      arg match {
        case StrValues.Open => Open()
        case StrValues.InProgress => InProgress()
        case StrValues.Approved => Approved()
        case StrValues.Consolidated => Consolidated()
        case StrValues.NotApproved => NotApproved()
        case StrValues.Rejected => Rejected()
        case StrValues.Closed => Closed()
        case _ => throw new IllegalArgumentException(s"Not a valid value for TaskState: $arg")
      }
    }
  }

  object AssignmentState extends StateConverters[AssignmentState] {

    import AssignmentStates._

    implicit val reads: Reads[AssignmentState] = __.read[String].map(o => asState(o))
    implicit val writes: Writes[AssignmentState] = Writes(state => JsString(asString(state)))

    override implicit def asState(arg: String): AssignmentState = {
      arg match {
        case StrValues.Available => Available()
        case StrValues.Assigned => Assigned()
        case StrValues.Completed => Completed()
        case StrValues.Aborted => Aborted()
        case _ => throw new IllegalArgumentException(s"Not a valid value for AssignmentState: $arg")
      }
    }
  }

}

