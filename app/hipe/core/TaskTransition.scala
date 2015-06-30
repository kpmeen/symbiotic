/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import hipe.core.States.TaskState
import hipe.core.StepDestinationCmd._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class TaskTransition(state: TaskState, dest: StepDestination)

object TaskTransition {

  implicit val format: Format[TaskTransition] = (
    (__ \ "state").format[TaskState] and
      (__ \ "dest").format[StepDestination]
    )(TaskTransition.apply, unlift(TaskTransition.unapply))

  def toBSON(t: TaskTransition): DBObject = {
    DBObject(
      "state" -> TaskState.asString(t.state),
      "dest" -> StepDestination.toBSON(t.dest)
    )
  }

  def fromBSON(dbo: DBObject): TaskTransition = {
    TaskTransition(
      state = TaskState.asState(dbo.as[String]("state")),
      dest = StepDestination.fromBSON(dbo.as[DBObject]("dest"))
    )
  }

}
