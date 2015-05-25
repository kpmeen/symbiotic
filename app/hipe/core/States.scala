/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import play.api.libs.json._

import scala.reflect._

object States {

  sealed trait State

  private object StrValues {
    val New = State.asString[New]()
    val Approved = State.asString[Approved]()
    val Consolidated = State.asString[Consolidated]()
    val Rejected = State.asString[Rejected]()
    val Closed = State.asString[Closed]()
  }

  object State {
    implicit val reads: Reads[State] = __.read[String].map(o => asTaskState(o))
    implicit val writes: Writes[State] = Writes {
      (a: State) => JsString(asString(Some(a)))
    }

    implicit def asString[A <: State](arg: Option[A] = None)(implicit ct: ClassTag[A]): String = {
      arg.fold(classTag[A].runtimeClass.getSimpleName)(_.getClass.getSimpleName)
    }

    implicit def asTaskState(arg: String): State = {
      arg match {
        case StrValues.New => New()
        case StrValues.Approved => Approved()
        case StrValues.Consolidated => Consolidated()
        case StrValues.Rejected => Rejected()
        case StrValues.Closed => Closed()
      }
    }
  }

  case class New() extends State

  case class Approved() extends State

  case class Consolidated() extends State

  case class Rejected() extends State

  case class Closed() extends State

  // ... what else?
}

