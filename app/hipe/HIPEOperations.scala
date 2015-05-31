/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

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
     * Appends a Step at the end of the process in a new separate StepGroup.
     *
     * @param proc the Process to append a step to
     * @param step the Step to append
     * @return the Process with the appended Step
     */
    def appendStep(proc: Process, step: Step): Process =
      proc.copy(stepGroups = proc.stepGroups ::: StepGroupList(StepGroup.create(step)))

    /**
     * Will append a Step to the end of the specified StepGroup.
     *
     * @param proc the Process
     * @param sgid the StepGroupId where the Step should be appended
     * @param step the Step to append
     * @return HIPEResult with the updated Process config
     */
    def appendStepToGroup(proc: Process, sgid: StepGroupId, step: Step): HIPEResult[Process] =
      proc.stepGroups.findWithPosition(sgid).map {
        case (sg: StepGroup, idx: Int) =>
          val nsg = sg.copy(steps = sg.steps ::: StepList(step))
          Right(proc.copy(stepGroups = proc.stepGroups.updated(idx, nsg)))
      }.getOrElse(Left(NotFound(s"Could not find StepGroup $sgid")))

    /**
     * Adds a Step in a new StepGroup in the process at the defined position.
     * The position is calculated from a flattened view of the steps in all
     * the step groups. If the index is larger than the current number of steps,
     * the Step is appended to the Process. If not it will be added at the given
     * index, shifting tailing steps to the right.
     *
     * @param proc the Process
     * @param step the Step to insert
     * @param pos the position to insert the Step in the list of steps
     * @return a Process with the new Step added to the list of steps
     */
    def insertStep(proc: Process, step: Step, pos: Int): Process =
      if (pos > proc.stepGroups.length) appendStep(proc, step)
      else proc.copy(stepGroups = proc.stepGroups.insert(StepGroup.create(step), pos))

    /**
     * Adds a Step to the specified StepGroup at the defined position. The position is
     * calculated based on size of the StepGroup. If the pos is larger than the number
     * steps in the group, the Step is appended to the Process. If not it will be added
     * at the given index, shifting tailing steps to the right.
     *
     * @param proc the Process
     * @param sgid the StepGroup to add a step to
     * @param step the Step to insert
     * @param pos the position in the StepGroup to insert the step
     * @return HIPEResult with the updated Process config
     */
    def insertStepToGroup(proc: Process, sgid: StepGroupId, step: Step, pos: Int): HIPEResult[Process] =
      proc.stepGroups.findWithPosition(sgid).map {
        case (sg: StepGroup, idx: Int) =>
          if (pos > sg.steps.length) {
            appendStepToGroup(proc, sgid, step)
          } else {
            val nsg = sg.copy(steps = sg.steps.insert(step, pos))
            Right(proc.copy(stepGroups = proc.stepGroups.updated(idx, nsg)))
          }
      }.getOrElse(Left(NotFound(s"Could not find StepGroup $sgid")))

    /**
     * Allows for re-arranging groups of steps in the process...
     *
     * currPos > newPos: the StepGroup is moved `before` its current location
     * currPos < newPos: the StepGroup is moved `after` its current location
     * currPos == newPos: nothing is changed
     *
     * @param proc the Process
     * @param sgid the StepGroup to move
     * @param newPos the new index position to place the StepGroup
     * @return HIPEResult with the updated process config
     */
    def moveStepGroup(proc: Process, sgid: StepGroupId, newPos: Int): HIPEResult[Process] =
      proc.stepGroups.findWithPosition(sgid).map {
        case (group: StepGroup, currPos: Int) =>
          if (currPos == newPos) Left(NotPossible(s"Old ($currPos) and new ($newPos) positions are the same..."))
          else Right(proc.copy(stepGroups = proc.stepGroups.move(currPos, newPos)))
      }.getOrElse(Left(NotFound(s"Could not find StepGroup $sgid")))

    /**
     * Allows for re-arranging steps inside their enclosing StepGroup.
     *
     * currPos > newPos: the Step is moved `before` its current location
     * currPos < newPos: the Step is moved `after` its current location
     * currPos == newPos: nothing is changed
     *
     * @param proc the Process
     * @param sgid the StepGroupId containing the step to move
     * @param sid the StepId to move
     * @param newPos the new position of the Step relative to the enclosing StepGroup
     * @return HIPEResult with the updated process config
     */
    def moveStepInGroup(proc: Process, sgid: StepGroupId, sid: StepId, newPos: Int): HIPEResult[Process] =
      proc.stepGroups.findWithPosition(sgid).map {
        case (group: StepGroup, grpPos: Int) => group.steps.findWithPosition(sid).map {
          case (step: Step, currPos: Int) =>
            if (currPos == newPos) {
              Left(NotPossible(s"Old ($currPos) and new ($newPos) positions are the same..."))
            } else {
              val nsg = group.copy(steps = group.steps.move(currPos, newPos))
              Right(proc.copy(stepGroups = proc.stepGroups.updated(grpPos, nsg)))
            }
        }.getOrElse(Left(NotFound(s"Could not find Step: $sid")))
      }.getOrElse(Left(NotFound(s"Could not find StepGroup: $sgid")))

    /**
     * Moves a Step out of its current StepGroup into the given position in the target StepGroup.
     *
     * @param proc the Process
     * @param stepId the Id of the step to move
     * @param toSgid the StepGroup to move to
     * @param posInGrp the position in the target StepGroup to move the step to
     * @return HIPEResult with the updated Process config
     */
    def moveStepToGroup(proc: Process, stepId: StepId, toSgid: StepGroupId, posInGrp: Int): HIPEResult[Process] =
      proc.stepGroups.findStep(stepId).map {
        case (group: StepGroup, step: Step) =>
          val p1 = proc.removeStep(group, step)
          insertStepToGroup(p1, toSgid, step, posInGrp)
      }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))

    /**
     * Will move a Step away from the enclosing StepGroup, and into a brand new StepGroup
     * at the given position in the process. If the original StepGroup is empty after the
     * move it will be removed from the process.
     *
     * @param proc the Process
     * @param stepId the StepIdId to move
     * @param newPos the position in the process where the new StepGroup will be added.
     * @return HIPEResult with the updated Process config
     */
    def moveStepToNewGroup(proc: Process, stepId: StepId, newPos: Int): HIPEResult[Process] =
      proc.stepGroups.findStep(stepId).map {
        case (group: StepGroup, step: Step) =>
          val p1 = proc.removeStep(group, step)
          Right(insertStep(p1, step, newPos - 1))
      }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))

    /**
     * ¡¡¡ WARNING !!!
     *
     * Removes the Step with the given stepId if it exists. If the Step is the single entry in the
     * enclosing StepGroup, the StepGroup is also removed.
     *
     * @param proc the Process to remove a step from
     * @param stepId the stepId to remove
     * @return HIPEResult with the updated Process config
     */
    def removeStep(proc: Process, stepId: StepId): HIPEResult[Process] =
      proc.stepGroups.findStep(stepId).map {
        case (group: StepGroup, step: Step) => Right(proc.removeStep(group, step))
      }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))

    /**
     * ¡¡¡ WARNING !!!
     *
     * Removes an entire StepGroup including the contained Step items.
     *
     * @param proc the Process
     * @param sgid the Id of the StepGroup to remove
     * @return
     */
    def removeGroup(proc: Process, sgid: StepGroupId): HIPEResult[Process] =
      proc.stepGroups.findWithPosition(sgid).map {
        case (group: StepGroup, pos: Int) => Right(proc.copy(stepGroups = proc.stepGroups.remove(pos)))
      }.getOrElse(Left(NotFound(s"Could not find group: $sgid")))
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

    private[hipe] def isTaskCompleted(task: Task, currStep: Step): Boolean =
      task.assignments.count(_.completed == true) >= currStep.minCompleted

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