/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.hipe

import play.api.libs.json.Json

/**
 * The interesting bit...a Task is what is moved around through the Steps during the Process life-cycle.
 */
case class Task(
  id: TaskId = TaskId(),
  processId: ProcessId,
  stepId: StepId,
  title: String,
  description: Option[String])

object Task {

  implicit val taskReads = Json.reads[Task]
  implicit val taskWrites = Json.writes[Task]
}
