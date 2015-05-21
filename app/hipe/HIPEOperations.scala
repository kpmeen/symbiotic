/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.AssignmentDetails.Assignment
import hipe.core.FailureTypes._
import hipe.core._
import models.parties.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

object HIPEOperations {

  type HIPEResult[A] = Either[FailedOp, A]

  /**
   * Functions that perform computations on Process data
   */
  trait ProcessOperations {

    private val logger = LoggerFactory.getLogger(classOf[ProcessOperations])

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
    def insertStep[A <: Step](proc: Process, step: A, index: Int): Process =
      if (index > proc.stepList.length) {
        appendStep(proc, step)
      } else {
        val lr = proc.stepList.splitAt(index)
        proc.copy(stepList = lr._1 ::: StepList(step) ::: lr._2)
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
    def moveStep(proc: Process, currIndex: Int, newIndex: Int): Process =
      if (currIndex == newIndex) {
        proc
      } else {
        val index = if (newIndex >= proc.stepList.length) proc.stepList.length - 1 else newIndex
        val lr = proc.stepList.splitAt(currIndex)
        val removed = (lr._1 ::: lr._2.tail).splitAt(index)

        proc.copy(stepList = removed._1 ::: List(proc.stepList(currIndex)) ::: removed._2)
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

    private val logger = LoggerFactory.getLogger(classOf[TaskOperations])

    implicit def flattenMoveResultOption(mr: Option[HIPEResult[Task]]): HIPEResult[Task] =
      mr.getOrElse(Left(NotPossible()))

    implicit def moveResultAsOption(mr: HIPEResult[Task]): Option[Task] =
      mr.fold(
        err => None,
        task => Some(task))

    /**
     * This function allows for moving a Task through the Process. If in a strict Process, the
     * movement will be restricted to the previous and next steps. If it is open, the task can
     * be moved anywhere.
     *
     * @param proc the Process to move the task within
     * @param newStepId The new StepId to move to
     * @return An option of Task. Will be None if the move was restricted.
     */
    def moveTask(proc: Process, task: Task, newStepId: StepId): HIPEResult[Task] = {
      val currStep = proc.step(task.stepId).get
      if (completed(task, currStep)) {
        proc.step(newStepId).map { s =>
          if (proc.strict) {
            prevNextSteps(proc, task.stepId) match {
              case PrevOrNext(prev, next) if newStepId == prev.id || newStepId == next.id => Right(initAssignments(task, s))
              case PrevOnly(prev) if newStepId == prev.id => Right(initAssignments(task, s))
              case NextOnly(next) if newStepId == next.id => Right(initAssignments(task, s))
              case _ => Left(NotAllowed(s"Moving to step $newStepId not possible...ignoring"))
            }
          } else {
            Right(initAssignments(task, s))
          }
        }
      } else {
        Left(Incomplete(s"Requires ${currStep.minCompleted} assignments to be completed"))
      }
    }

    def moveToNext(proc: Process, task: Task): HIPEResult[Task] =
      proc.stepList.nextStepFrom(task.stepId).map(s => moveTask(proc, task, s.id)).getOrElse {
        Left(NotFound(s"Could not find next step for ${task.stepId}"))
      }

    def moveToPrevious(proc: Process, task: Task): HIPEResult[Task] =
      proc.stepList.previousStepFrom(task.stepId).map(s => moveTask(proc, task, s.id)).getOrElse {
        Left(NotFound(s"Could not find previous step for ${task.stepId}"))
      }

    def addTaskToProcess(proc: Process, taskTitle: String, taskDesc: Option[String]): Option[Task] =
      proc.stepList.headOption.map { step =>
        val t = Task(
          processId = proc.id.get,
          stepId = step.id,
          title = taskTitle,
          description = taskDesc)
        initAssignments(t, proc.stepList.head)
      }

    def addTaskToProcess(proc: Process, task: Task): Option[Task] =
      proc.stepList.headOption.map { step =>
        // FIXME: Hacking the ID for now...see issue #9 in gitlab
        val t = task.copy(id = Some(TaskId.create()), processId = proc.id.get, stepId = step.id)
        initAssignments(t, proc.stepList.head)
      }

    /**
     * Calculates the surroundings for the current Step for a "strict" process
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

    private[hipe] def completed(task: Task, currStep: Step): Boolean =
      task.assignments.count(_.completed == true) >= currStep.minCompleted

    /*
      TODO: If a task has open assignments... should it be possible to move?
          In strict proc I would say no, unless overridden.
    */
    private[hipe] def initAssignments(task: Task, toStep: Step): Task = {
      val assigns = Seq.newBuilder[Assignment]
      for (i <- 0 to toStep.minAssignments - 1) {
        assigns += Assignment()
      }
      task.copy(
        stepId = toStep.id,
        assignments = assigns.result())
    }

    /*
      TODO: Change return types to ensure validation errors are clear and obvious. Use Scalaz Validation perhaps?
    */
    def assign(task: Task, assignTo: UserId): Task =
      assignmentApply(task, assignTo)(
        cond = t => !t.assignments.exists(_.assignee.contains(assignTo)),
        cp = _.filterNot(_.completed)
          .find(_.assignee.isEmpty)
          .map(a => a.copy(assignee = Some(assignTo), assignedDate = Some(DateTime.now)))
      )

    def completeAssignment(task: Task, assignee: UserId): Task =
      assignmentApply(task, assignee)(
        cond = _.assignments.exists(_.assignee.contains(assignee)),
        cp = _.filterNot(_.completed)
          .find(_.assignee.contains(assignee))
          .map(a => a.copy(completed = true, completionDate = Some(DateTime.now)))
      )

    private[this] def assignmentApply(task: Task, uid: UserId)(cond: Task => Boolean, cp: Seq[Assignment] => Option[Assignment]): Task = {
      val ass = if (cond(task)) task.updateAssignment(ass => cp(ass)) else task.assignments
      task.copy(assignments = ass)
    }
  }

}