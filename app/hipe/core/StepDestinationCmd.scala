/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import play.api.libs.json._

object StepDestinationCmd {

  sealed trait StepDestination {
    val key: String
  }

  case class Next() extends StepDestination {
    override val key: String = "Next"
  }

  case class Prev() extends StepDestination {
    override val key: String = "Prev"
  }

  case class Goto(stepId: StepId) extends StepDestination {
    override val key: String = "Goto"
  }

  private object StrValues {
    val Next = "Next"
    val Prev = "Prev"
    val Goto = "Goto"
  }

  object StepDestination {

    // FIXME: WTF??? Why do I get the _entire_ JSON in here, it should only get the subset? hmm...
    implicit val reads: Reads[StepDestination] = Reads {
      case value: JsValue =>
        val curr = (value \ "dest").as[JsValue]
        (curr \ "key").asOpt[String].map {
          case StrValues.Next => JsSuccess(Next())
          case StrValues.Prev => JsSuccess(Prev())
          case StrValues.Goto => JsSuccess(Goto((curr \ "stepId").as[String]))
          case err => JsError(s"Illegal step destination $err")
        }.getOrElse {
          JsError(s"Step destination key not defined in $value")
        }
    }

    implicit val writes: Writes[StepDestination] = Writes {
      case next: Next => Json.obj("key" -> next.key)
      case prev: Prev => Json.obj("key" -> prev.key)
      case goto: Goto => Json.obj("key" -> goto.key, "stepId" -> goto.stepId)
      case err => throw new IllegalStateException(s"Cannot write $err because it's not a valid StepDestination")
    }

    def fromBSON(dbo: DBObject): StepDestination =
      dbo.getAs[String]("key").map {
        case StrValues.Next => Next()
        case StrValues.Prev => Prev()
        case StrValues.Goto => Goto(dbo.as[String]("stepId"))
        case err => throw new IllegalStateException(s"Illegal step destination $err")
      }.getOrElse {
        throw new IllegalStateException(s"Illegal step destination key in $dbo")
      }

    def toBSON(dst: StepDestination): DBObject = {
      dst match {
        case g: Goto => DBObject("key" -> g.key, "stepId" -> g.stepId.value)
        case _ => DBObject("key" -> dst.key)
      }
    }

  }

}
