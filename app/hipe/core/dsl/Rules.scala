/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core.dsl

import com.mongodb.casbah.commons.Imports._
import hipe.core.States.TaskState
import hipe.core.StepDestinationCmd
import hipe.core.dsl.Rules.TransitionRule
import hipe.core.dsl.TransitionDSL.Parser.{NoSuccess, Success, parseAll, transition}
import play.api.libs.json._

object Rules {

  sealed trait DSLRule {
    val rule: String
  }

  case class TransitionRule(rule: String) extends DSLRule {

    def exec: Either[TransitionParseError, TransitionParseResult] =
      parseAll(transition, rule).map(x => TransitionParseResult(x._1, x._2)) match {
        case Success(trr, in) => Right(trr)
        case NoSuccess(msg, in) => Left(TransitionParseError(msg, in.source.toString, in.pos.column))
      }

  }

  object TransitionRule {
    implicit val reads: Reads[TransitionRule] = __.read[String].map(o => TransitionRule(o))
    implicit val writes: Writes[TransitionRule] = Writes {
      (a: TransitionRule) => JsString(a.rule)
    }
  }

  case class TransitionParseError(msg: String, source: String, pos: Int)

  object TransitionParseError {
    implicit val format: Format[TransitionParseError] = Json.format[TransitionParseError]
  }

  case class TransitionParseResult(ts: TaskState, sd: StepDestinationCmd.StepDestination)

}

case class TaskStateRule(taskState: TaskState, transitionRule: Rules.TransitionRule)

object TaskStateRule {

  implicit val reads: Reads[TaskStateRule] = Json.reads[TaskStateRule]
  implicit val writes: Writes[TaskStateRule] = Json.writes[TaskStateRule]

  def toBSON(tsr: TaskStateRule): DBObject =
    MongoDBObject(
      "taskState" -> TaskState.asString(tsr.taskState),
      "rule" -> tsr.transitionRule.rule
    )

  def fromBSON(dbo: DBObject): TaskStateRule =
    TaskStateRule(
      taskState = dbo.as[String]("taskState"),
      transitionRule = TransitionRule(dbo.as[String]("rule"))
    )

}

