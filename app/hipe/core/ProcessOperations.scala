/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import models.parties.UserId

/**
 * Functions that perform computations on Process data
 */
trait ProcessOperations {

  /**
   * Appends a Step to a Process.
   */
  def appendStep[A <: Step](proc: Process, step: A): Process = proc.copy(steps = proc.steps ::: List[Step](step))

  /**
   * Inserts a Step on the board at the defined index. If the index is larger than the current number of steps, the
   * Step is appended to the Process. If not it will be added at the given index, shifting tailing steps to the right.
   *
   * @param proc the Process to add a step to
   * @param step the Step to insert
   * @param index the position to insert the Step in the list of steps
   * @return a Process with the new Step added to the list of steps
   */
  def insertStep[A <: Step](proc: Process, step: A, index: Int): Process = {
    if (index > proc.steps.length) {
      appendStep(proc, step)
    } else {
      val lr = proc.steps.splitAt(index)
      proc.copy(steps = lr._1 ::: List(step) ::: lr._2)
    }
  }

  /**
   * Allows for re-arranging steps in the process...
   *
   * currIndex > newIndex: the Step is moved `before` its current location
   * currIndex < newIndex: the Step is moved `after` its current location
   * currIndex == newIndex: the steps are left alone
   *
   * @param proc Process where the steps should be moved
   * @param currIndex the current index position of the Step
   * @param newIndex the new index position to place the Step
   * @return A Process with an updated step order
   */
  def moveStep(proc: Process, currIndex: Int, newIndex: Int): Process = {
    if (currIndex == newIndex) {
      proc
    } else {
      val index = if (newIndex >= proc.steps.length) proc.steps.length - 1 else newIndex
      val lr = proc.steps.splitAt(currIndex)
      val removed = (lr._1 ::: lr._2.tail).splitAt(index)

      proc.copy(steps = removed._1 ::: List(proc.steps(currIndex)) ::: removed._2)
    }
  }

  /**
   * ¡¡¡ WARNING !!!
   *
   * Removes the Step at the given index if the index number is lower or equal to the number of steps, and if the
   * Step to be removed does not contain any Tasks.
   *
   * @param proc the Process to remove a step from
   * @param stepIndex the step index to remove
   * @param findTasks function to identify which tasks belong to the given stepId on the given processId.
   * @return Some[Process] if the Step was removed, otherwise None
   */
  def removeStep(proc: Process, stepIndex: Int)(findTasks: (ProcessId, StepId) => List[Task]): Option[Process] = {
    if (stepIndex < proc.steps.length) {
      if (proc.steps.isDefinedAt(stepIndex)) {
        // Locate any tasks that are associated with the given step.
        val tasks = findTasks(proc.id.get, proc.steps(stepIndex).id)
        if (tasks.isEmpty) {
          val lr = proc.steps.splitAt(stepIndex)
          return Some(proc.copy(steps = lr._1 ::: lr._2.tail))
        }
      }
    }
    None
  }
}

/**
 * Functions that perform computations on Task data
 */
trait TaskOperations {

  /**
   * This function allows for moving a Task through the Process. If in a strict Process, the
   * movement will be restricted to the previous and next steps. If it is open, the task can
   * be moved anywhere.
   *
   * @param proc the Process to move the task within
   * @param newStepId The new StepId to move to
   * @return An option of Task. Will be None if the move was restricted.
   */
  def moveTask(proc: Process, task: Task, newStepId: StepId): Option[Task] = {
    if (proc.strict) {
      prevNextSteps(proc, task.stepId) match {
        case PrevOrNext(prev, next) if newStepId == prev || newStepId == next => Some(task.copy(stepId = newStepId))
        case PrevOnly(prev) if newStepId == prev => Some(task.copy(stepId = newStepId))
        case NextOnly(next) if newStepId == next => Some(task.copy(stepId = newStepId))
        case _ => None
      }
    } else {
      Some(task.copy(stepId = newStepId))
    }
  }

  def moveToNext(proc: Process, task: Task): Option[Task] = {
    proc.nextStepFrom(task.stepId).flatMap(s => moveTask(proc, task, s.id))
  }

  def moveToPrevious(proc: Process, task: Task): Option[Task] = {
    proc.previousStepFrom(task.stepId).flatMap(s => moveTask(proc, task, s.id))
  }

  /**
   * Adds a new Task to the first step of the Process.
   *
   * @param proc Process to add task to
   * @param taskTitle the title of the Task to add
   * @param taskDesc the description of the Task to add
   * @return an Option[Task]
   */
  def addTaskToProcess(proc: Process, taskTitle: String, taskDesc: Option[String]): Option[Task] =
    proc.steps.headOption.flatMap(col => Some(
      Task(
        processId = proc.id.get,
        stepId = col.id,
        title = taskTitle,
        description = taskDesc
      )
    ))

  /**
   * Will add a new Task to the first step of the Process.
   *
   * @param proc Process to add task to
   * @param task the Task to add
   * @return an Option[Task]
   */
  def addTaskToProcess(proc: Process, task: Task): Option[Task] = {
    proc.steps.headOption.flatMap(col => Some(task.copy(processId = proc.id.get, stepId = col.id)))
  }

  /**
   * Will assign the Task with taskId to the provided UserId and return the updated data.
   *
   * @param assignTo The user to assign the Task to
   * @param taskId The task ID to assign
   * @param find A function for locating the task with the provided ID
   * @return an Option[Task]
   */
  def assignTask(assignTo: UserId, taskId: TaskId)(find: (TaskId) => Option[Task]): Option[Task] = {
    find(taskId).map(task => task.copy(assignee = Some(assignTo)))
  }

  /**
   * Calculates the surroundings for the current Step in a process
   *
   * @param proc Process to check
   * @param currStep the current StepId
   * @return a type of PrevNextStepType that may or may not have previous and/or next Step references.
   */
  private[hipe] def prevNextSteps(proc: Process, currStep: StepId): SurroundingSteps = {
    val currIndex = proc.steps.indexWhere(_.id == currStep)

    if (currIndex == 0) {
      NextOnly(proc.steps(1).id)
    } else if (currIndex == proc.steps.length - 1) {
      PrevOnly(proc.steps(proc.steps.length - 2).id)
    } else {
      PrevOrNext(proc.steps(currIndex - 1).id, proc.steps(currIndex + 1).id)
    }
  }
}
