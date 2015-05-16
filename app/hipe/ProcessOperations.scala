/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.AssignmentDetails.Assignment
import hipe.core._
import models.parties.UserId

/**
 * Functions that perform computations on Process data
 */
trait ProcessOperations {

  /**
   * Appends a Step to a Process.
   */
  def appendStep[A <: Step](proc: Process, step: A): Process = proc.copy(stepList = proc.stepList ::: StepList(step))

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
    if (index > proc.stepList.length) {
      appendStep(proc, step)
    } else {
      val lr = proc.stepList.splitAt(index)
      proc.copy(stepList = lr._1 ::: StepList(step) ::: lr._2)
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
      val index = if (newIndex >= proc.stepList.length) proc.stepList.length - 1 else newIndex
      val lr = proc.stepList.splitAt(currIndex)
      val removed = (lr._1 ::: lr._2.tail).splitAt(index)

      proc.copy(stepList = removed._1 ::: List(proc.stepList(currIndex)) ::: removed._2)
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
    if (stepIndex < proc.stepList.length) {
      if (proc.stepList.isDefinedAt(stepIndex)) {
        // Locate any tasks that are associated with the given step.
        val tasks = findTasks(proc.id.get, proc.stepList(stepIndex).id)
        if (tasks.isEmpty) {
          val lr = proc.stepList.splitAt(stepIndex)
          return Some(proc.copy(stepList = lr._1 ::: lr._2.tail))
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
    proc.step(newStepId).flatMap { s =>
      if (proc.strict) {
        prevNextSteps(proc, task.stepId) match {
          case PrevOrNext(prev, next) if newStepId == prev.id || newStepId == next.id => Some(assignForStep(task, s))
          case PrevOnly(prev) if newStepId == prev.id => Some(assignForStep(task, s))
          case NextOnly(next) if newStepId == next.id => Some(assignForStep(task, s))
          case _ => None
        }
      } else {
        Some(assignForStep(task, s))
      }
    }
  }

  /*
    TODO: If a task has open assignments... should it be possible to move? In strict proc I would say no, unless overridden.
  */

  private def assignForStep(task: Task, toStep: Step): Task = {
    val assigns = Seq.newBuilder[Assignment]
    for (i <- 0 to toStep.minNumAssignments - 1) {
      assigns += Assignment()
    }
    task.copy(
      stepId = toStep.id,
      assignments = assigns.result()
    )
  }

  def moveToNext(proc: Process, task: Task): Option[Task] = {
    proc.stepList.nextStepFrom(task.stepId).flatMap(s => moveTask(proc, task, s.id))
  }

  def moveToPrevious(proc: Process, task: Task): Option[Task] = {
    proc.stepList.previousStepFrom(task.stepId).flatMap(s => moveTask(proc, task, s.id))
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
    proc.stepList.headOption.map { step =>
      val t = Task(
        processId = proc.id.get,
        stepId = step.id,
        title = taskTitle,
        description = taskDesc
      )
      assignForStep(t, proc.stepList.head)
    }

  /**
   * Will add a new Task to the first step of the Process.
   *
   * @param proc Process to add task to
   * @param task the Task to add
   * @return an Option[Task]
   */
  def addTaskToProcess(proc: Process, task: Task): Option[Task] = {
    proc.stepList.headOption.map(step => task.copy(processId = proc.id.get, stepId = step.id))
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
    ??? //find(taskId).map(task => task.copy(assignee = Some(assignTo)))
  }

  /**
   * Calculates the surroundings for the current Step in a process
   *
   * @param proc Process to check
   * @param currStep the current StepId
   * @return a type of PrevNextStepType that may or may not have previous and/or next Step references.
   */
  private[hipe] def prevNextSteps(proc: Process, currStep: StepId): SurroundingSteps = {
    val currIndex = proc.stepList.steps.indexWhere(_.id == currStep)

    if (currIndex == 0) {
      NextOnly(proc.stepList(1))
    } else if (currIndex == proc.stepList.length - 1) {
      PrevOnly(proc.stepList(proc.stepList.length - 2))
    } else {
      PrevOrNext(proc.stepList(currIndex - 1), proc.stepList(currIndex + 1))
    }
  }
}
