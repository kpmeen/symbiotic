/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.hipe.engine

import play.api.libs.json.Json

/**
 * The interesting bit...a Task is what is moved around through the Steps during the Process life-cycle.
 */
case class Task(
  id: TaskId = TaskId(),
  processId: ProcessId,
  stepId: StepId,
  title: String,
  description: Option[String]) {

  /**
   * This function allows for moving a Task through the Process. If in a strict Process, the movement will be restricted
   * to the previous and next steps. If it is open, the task can be moved anywhere.
   *
   * @param newStepId The new StepId to move to
   * @param findProcess function that returns a Process based on ProcessId
   * @return An option of Task. Will be None if the move was restricted.
   */
  def move(newStepId: StepId)(findProcess: (ProcessId) => Option[Process]): Option[Task] = {
    findProcess(processId).flatMap { b =>
      if (b.strict) {
        b.prevNextSteps(stepId) match {
          case PrevNextStep(prev, next) if newStepId == prev || newStepId == next => Some(this.copy(stepId = newStepId))
          case PrevOnlyStep(prev) if newStepId == prev => Some(this.copy(stepId = newStepId))
          case NextOnlyStep(next) if newStepId == next => Some(this.copy(stepId = newStepId))
          case _ => None
        }
      } else {
        Some(this.copy(stepId = newStepId))
      }
    }
  }

}

object Task {

  implicit val taskReads = Json.reads[Task]
  implicit val taskWrites = Json.writes[Task]

  /**
   * Adds a new Task to the Process in the left-most column.
   *
   * @param title the title of the Task to add
   * @param desc the description of the Task to add
   * @return an Option[Task]
   */
  def addToProcess(board: Process, title: String, desc: Option[String]): Option[Task] =
    board.steps.headOption.flatMap(col => Some(
      Task(
        processId = board.id,
        stepId = col.id,
        title = title,
        description = desc
      )
    ))
}
