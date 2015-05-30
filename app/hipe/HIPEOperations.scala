/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.AssignmentDetails.Assignment
import hipe.core.FailureTypes._
import hipe.core.States.{AssignmentState, AssignmentStates, TaskStates}
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

    /**
     * Appends a step at the end of the process in a new separate StepGroup.
     *
     * @param proc the Process to append a step to
     * @param step the Step to append
     * @return the Process with the appended Step
     */
    def appendStep(proc: Process, step: Step): Process = {
      proc.copy(stepGroups = proc.stepGroups ::: StepGroupList(StepGroup.create(step)))
    }

    def appendStepToGroup(proc: Process, sgid: StepGroupId, step: Step): Process = {
      proc.stepGroups.findWithIndex(sgid).map {
        case (sg: StepGroup, idx: Int) =>
          val nsg = sg.copy(steps = sg.steps ::: StepList(step))
          proc.copy(stepGroups = proc.stepGroups.updated(idx, nsg))
      }.getOrElse(proc)
    }

    /**
     * Adds a Step in a new StepGroup in the process at the defined position.
     * The position is calculated from a flattened view of the steps in all
     * the step groups. If the index is larger than the current number of steps,
     * the Step is appended to the Process. If not it will be added at the given
     * index, shifting tailing steps to the right.
     *
     * @param proc the Process to add a step to
     * @param step the Step to insert
     * @param pos the position to insert the Step in the list of steps
     * @return a Process with the new Step added to the list of steps
     */
    def insertStep(proc: Process, step: Step, pos: Int): Process =
      if (pos > proc.stepGroups.length) {
        appendStep(proc, step)
      } else {
        val lr = proc.stepGroups.splitAt(pos)
        proc.copy(stepGroups = lr._1 ::: StepGroupList(StepGroup.create(step)) ::: lr._2)
      }

    def insertStepToGroup(proc: Process, sgid: StepGroupId, step: Step, groupPos: Int): Process = {
      proc.stepGroups.findWithIndex(sgid).map {
        case (sg: StepGroup, idx: Int) =>
          if (groupPos > sg.steps.length) {
            appendStepToGroup(proc, sgid, step)
          } else {
            val lr = sg.steps.splitAt(idx)
            val nsg = sg.copy(steps = lr._1 ::: StepList(step) ::: lr._2)
            proc.copy(stepGroups = proc.stepGroups.updated(idx, nsg))
          }
      }.getOrElse(proc)
    }

    /**
     * Allows for re-arranging groups of steps in the process...
     *
     * currIndex > newIndex: the StepGroup is moved `before` its current location
     * currIndex < newIndex: the StepGroup is moved `after` its current location
     * currIndex == newIndex: nothing is changed
     *
     * @param proc Process to re-arrange the groups should be moved
     * @param currPos the current index position of the StepGroup
     * @param newPos the new index position to place the StepGroup
     * @return A Process with an updated StepGroup order
     */
    def moveStepGroup(proc: Process, currPos: Int, newPos: Int): Process =
      if (currPos == newPos) {
        proc
      } else {
        val index = if (newPos >= proc.stepGroups.length) proc.stepGroups.length - 1 else newPos
        val lr = proc.stepGroups.splitAt(currPos)
        val removed = (lr._1 ::: lr._2.tail).splitAt(index)

        proc.copy(stepGroups = removed._1 ::: StepGroupList(proc.stepGroups(currPos)) ::: removed._2)
      }

    /**
     *
     * @param proc
     * @param sgid
     * @param currPos the current position of the Step relative to the enclosing StepGroup
     * @param newPos the new position of the Step relative to the enclosing StepGroup
     * @return the updated Process config
     */
    def moveStepInGroup(proc: Process, sgid: StepGroupId, currPos: Int, newPos: Int): Process = {
      if (currPos == newPos) {
        proc
      } else {
        proc.stepGroups.findWithIndex(sgid).map {
          case (sg: StepGroup, idx: Int) =>
            ???
        }.getOrElse(proc)
      }
    }

    def moveStepToGroup = ???

    /**
     * ¡¡¡ WARNING !!!
     *
     * Removes the Step with the given stepId if it exists. If the Step is the single entry in the
     * enclosing StepGroup, the StepGroup is also removed. The Step is only removed if there are no
     * active Tasks referencing the given StepId, to prevent process corruption.
     *
     * @param proc the Process to remove a step from
     * @param stepId the stepId to remove
     * @param findTasks function to identify which tasks belong to the given stepId on the given processId.
     * @return HIPEResult[Process]
     */
    def removeStep(proc: Process, stepId: StepId)(findTasks: (ProcessId, StepId) => List[Task]): HIPEResult[Process] = {
      proc.stepGroups.findStep(stepId).map {
        case (group: StepGroup, step: Step) =>
          val grpIndex = proc.stepGroups.indexWhere(_.id == group.id)
          val tasks = findTasks(proc.id.get, step.id.get)
          if (tasks.isEmpty) {
            if (group.steps.size == 1) {
              val lr = proc.stepGroups.splitAt(grpIndex)
              Right(proc.copy(stepGroups = lr._1 ::: lr._2.tail))
            } else {
              val lr = group.steps.splitAt(group.steps.indexWhere(_.id == stepId))
              val grp = group.copy(steps = lr._1 ::: lr._2.tail)
              Right(proc.copy(stepGroups = proc.stepGroups.updated(grpIndex, grp)))
            }
          } else {
            Left(NotAllowed("It is not allowed to move a Step that contain active Tasks."))
          }
      }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))
    }

    def removeGroup(proc: Process, sgid: StepGroupId)(findTasks: (ProcessId, StepId) => List[Task]): HIPEResult[Process] = {
      ???
    }
  }

  /**
   * Functions that perform computations on Task data
   */
  trait TaskOperations {

    private val logger = LoggerFactory.getLogger(classOf[TaskOperations])

    implicit def flattenMoveResultOption(mr: Option[HIPEResult[Task]]): HIPEResult[Task] =
      mr.getOrElse(Left(NotPossible("Result does not contain data and cannot be flattened.")))

    implicit def moveResultAsOption(mr: HIPEResult[Task]): Option[Task] =
      mr.fold(
        err => None,
        task => Some(task)
      )

    /**
     * Calculates the surroundings for the current Step for a "strict" process
     *
     * @param proc Process to check
     * @param currStep the current StepId
     * @return a type of PrevNextStepType that may or may not have previous and/or next Step references.
     */
    private[hipe] def prevNextSteps(proc: Process, currStep: StepId): SurroundingSteps = {
      val steps = proc.stepGroups.flatten
      val currPos = steps.indexWhere(_.id.contains(currStep))

      if (currPos == 0) {
        NextOnly(steps(1))
      } else if (currPos == steps.length - 1) {
        PrevOnly(steps(steps.length - 2))
      } else {
        PrevOrNext(steps(currPos - 1), steps(currPos + 1))
      }
    }

    private[hipe] def isTaskCompleted(task: Task, currStep: Step): Boolean = {
      task.assignments.count(_.completed == true) >= currStep.minCompleted
    }

    private[hipe] def initAssignments(task: Task, toStep: Step): Task = {
      val assigns = Seq.newBuilder[Assignment]
      for (i <- 0 to toStep.minAssignments - 1) {
        assigns += Assignment()
      }
      task.copy(
        stepId = toStep.id.get,
        assignments = assigns.result())
    }

    private[this] def assignmentApply(task: Task, uid: UserId)(cond: Task => Boolean, cp: Seq[Assignment] => Option[Assignment]): Task = {
      val ass = if (cond(task)) task.updateAssignment(ass => cp(ass)) else task.assignments
      task.copy(assignments = ass)
    }

    private[this] def assignmentStateApply(assignment: Assignment, state: AssignmentState): Assignment =
      state match {
        case a: AssignmentStates.Assigned => assignment.copy(status = a, assignedDate = Some(DateTime.now))
        case c: AssignmentStates.Completed => assignment.copy(status = c, completionDate = Some(DateTime.now))
        case s => assignment.copy(status = s)
      }

    /**
     * This function allows for moving a Task through the Process. If in a strict
     * Process, the movement will be restricted to the previous and next steps.
     * If it is open, the task can be moved anywhere.
     *
     * @param proc the Process to move the task within
     * @param newStepId The new StepId to move to
     * @return A HIPEResult of Task. Will have a Left value if the move was restricted.
     */
    def moveTask(proc: Process, task: Task, newStepId: StepId): HIPEResult[Task] = {
      val currStep = proc.step(task.stepId).get
      if (isTaskCompleted(task, currStep)) {
        proc.step(newStepId).map { s =>
          if (proc.strict) {
            prevNextSteps(proc, task.stepId) match {
              case PrevOrNext(prev, next) if prev.id.contains(newStepId) || next.id.contains(newStepId) => Right(initAssignments(task, s))
              case PrevOnly(prev) if prev.id.contains(newStepId) => Right(initAssignments(task, s))
              case NextOnly(next) if next.id.contains(newStepId) => Right(initAssignments(task, s))
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
      proc.stepGroups.nextStepFrom(task.stepId).map(s => moveTask(proc, task, s.id.get)).getOrElse {
        Left(NotFound(s"Could not find next step for ${task.stepId}"))
      }

    def moveToPrevious(proc: Process, task: Task): HIPEResult[Task] =
      proc.stepGroups.previousStepFrom(task.stepId).map(s => moveTask(proc, task, s.id.get)).getOrElse {
        Left(NotFound(s"Could not find previous step for ${task.stepId}"))
      }

    def createTask(proc: Process, taskTitle: String, taskDesc: Option[String]): Option[Task] =
      for {
        step <- proc.stepGroups.headOption.flatMap(_.steps.headOption)
        pid <- proc.id
        sid <- step.id
      } yield {
        val t = Task(
          processId = pid,
          stepId = sid,
          title = taskTitle,
          description = taskDesc,
          state = TaskStates.Open()
        )
        initAssignments(t, step)
      }

    def createTask(proc: Process, task: Task): Option[Task] =
      for {
        step <- proc.stepGroups.headOption.flatMap(_.steps.headOption)
        pid <- proc.id
        sid <- step.id
      } yield {
        val t = task.copy(id = Some(TaskId.create()), processId = pid, stepId = sid, state = TaskStates.Open())
        initAssignments(t, step)
      }

    def assign(task: Task, assignTo: UserId): Task =
      assignmentApply(task, assignTo)(
        cond = t => !t.assignments.exists(_.assignee.contains(assignTo)),
        cp = _.filterNot(_.completed).find(_.assignee.isEmpty).map { a =>
          assignmentStateApply(
            a.copy(assignee = Some(assignTo)),
            AssignmentStates.Assigned()
          )
        }
      )

    def completeAssignment(task: Task, assignee: UserId): Task =
      assignmentApply(task, assignee)(
        cond = _.assignments.exists(_.assignee.contains(assignee)),
        cp = _.filterNot(_.completed).find(_.assignee.contains(assignee)).map { a =>
          assignmentStateApply(a, AssignmentStates.Completed())
        }
      )

    def completeAll(task: Task): Task = {
      val assignments = task.assignments.map(a => assignmentStateApply(a, AssignmentStates.Completed()))
      task.copy(assignments = assignments)
    }

    def approve(proc: Process, task: Task): HIPEResult[Task] = {
      // TODO: Move to correct step as defined in DSL...for now, move to next!!!
      moveToNext(proc, task.copy(state = TaskStates.Approved()))
    }

    def reject(proc: Process, task: Task): HIPEResult[Task] = {
      val assigns = task.assignments.map(a => assignmentStateApply(a, AssignmentStates.Aborted()))
      val t = task.copy(state = TaskStates.Rejected(), assignments = assigns)
      //      move(t)
      // TODO: ¡¡¡Move task to the appropriate Step. Complete once DSL is finished...for now, move to previous!!!
      moveToPrevious(proc, t)
    }

  }

}