/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import play.api.libs.json._

import scala.reflect._

object States {

  sealed trait State

  object State {
    implicit val reads: Reads[State] = __.read[String].map(o => asTaskState(o))
    implicit val writes: Writes[State] = Writes {
      (a: State) => JsString(asString(Some(a)))
    }
  }

  def asString[A <: State](arg: Option[A] = None)(implicit ct: ClassTag[A]): String = {
    arg.fold(classTag[A].runtimeClass.getSimpleName)(_.getClass.getSimpleName)
  }

  private object StrValues {
    val Accepted = asString[Accepted]()
    val Approved = asString[Approved]()
    val Assigned = asString[Assigned]()
    val Consolidated = asString[Consolidated]()
    val Delegated = asString[Delegated]()
    val Rejected = asString[Rejected]()
    val Unassigned = asString[Unassigned]()
  }

  def asTaskState(arg: String): State = {
    arg match {
      case StrValues.Accepted => Accepted()
      case StrValues.Approved => Approved()
      case StrValues.Assigned => Assigned()
      case StrValues.Delegated => Delegated()
      case StrValues.Consolidated => Consolidated()
      case StrValues.Rejected => Rejected()
      case StrValues.Unassigned => Unassigned()
    }
  }

  case class Accepted() extends State

  case class Approved() extends State

  case class Assigned() extends State

  case class Consolidated() extends State

  case class Delegated() extends State

  case class Rejected() extends State

  case class Unassigned() extends State

  // TODO ... what else?
}

