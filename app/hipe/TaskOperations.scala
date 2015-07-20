/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.Implicits._
import hipe.core.FailureTypes._
import hipe.core.States.{AssignmentStates, TaskStates}
import hipe.core.StepDestinationCmd.{Goto, Next, Prev}
import hipe.core._
import models.base.PersistentType.{UserStamp, VersionStamp}
import models.parties.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Functions that perform computations on Task data
 */
private[hipe] trait TaskOperations {

  private val logger = LoggerFactory.getLogger(classOf[TaskOperations])

  /**
   * This function allows for moving a Task through the Process. If in a strict
   * Process, the movement will be restricted to the previous and/or next step. If
   * the overrideStrict flag is set to true, the default strict rules will be ignored,
   * and the move will be executed as if the process was open.
   *
   * If it is open, the task can be moved anywhere. Even for open processes, if
   * the minCompleted assignments criteria is configured but not met, the move
   * will not be allowed.
   *
   * @param p the Process to move the task within
   * @param t the Task to move
   * @param curr the current Step
   * @param next the target Step
   * @param overrideStrict set to true if you want to override default rules for strict processes
   *
   * @return A HIPEResult of Task. Will have a Left value if the move was restricted.
   */
  private[this] def mv(p: Process, t: Task, curr: Step, next: Step, overrideStrict: Boolean = false): HIPEResult[Task] = {
    if (t.isTaskCompleted(curr)) {
      if (p.strict && !overrideStrict) {
        val target = next.id.get
        p.prevNextSteps(t.stepId) match {
          case PrevOrNext(prv, nxt) if prv.id.contains(target) || nxt.id.contains(target) =>
            Right(t.prepareFor(next))

          case PrevOnly(prv) if prv.id.contains(target) =>
            Right(t.prepareFor(next))

          case NextOnly(nxt) if nxt.id.contains(target) =>
            Right(t.prepareFor(next))

          case _ =>
            Left(NotAllowed(s"Moving to step $target not possible...ignoring"))
        }
      }
      else {
        Right(t.prepareFor(next))
      }
    } else {
      Left(Incomplete(s"Requires ${curr.minCompleted} assignments to be completed"))
    }
  }

  /**
   * This function will move a Task to the current Steps immediate (right) Next step.
   *
   * @param p the Process
   * @param t the Task to move
   * @param curr the current Step
   * @return A HIPEResult of Task. Will have a Left value if the move was restricted.
   */
  protected def toNext(p: Process, t: Task, curr: Step): HIPEResult[Task] =
    p.stepGroups.nextStepFrom(t.stepId).map(s => mv(p, t, curr, s)).getOrElse {
      Left(NotFound(s"Could not find next step for ${t.stepId}"))
    }

  /**
   * This function will move a Task to the current Steps immediate (left) previous step.
   *
   * @param p the Process
   * @param t the Task to move
   * @param curr the current Step
   * @return A HIPEResult of Task. Will have a Left value if the move was restricted.
   */
  protected def toPrev(p: Process, t: Task, curr: Step): HIPEResult[Task] =
    p.stepGroups.previousStepFrom(t.stepId).map(s => mv(p, t, curr, s)).getOrElse {
      Left(NotFound(s"Could not find previous step for ${t.stepId}"))
    }

  /**
   * Performs a {{{mv}}} operation on the task by first validating the transition rules defined
   * on the current Step. If an appropriate rule is found, the corresponding move will be executed.
   *
   * In the case where no rules are found, the move operation defined in the fallback function will
   * be executed instead.
   *
   * @param proc the Process
   * @param task the Task to move
   * @param fallback function defining an alternative move function if no transition rules are defined.
   *
   * @return A HIPEResult of Task. Will have a Left value if the move was not possible.
   */
  protected def transition(proc: Process, task: Task)(fallback: (Step) => HIPEResult[Task]) = {
    val curr = proc.step(task.stepId).get
    curr.transitionRules.flatMap(_.find(_.state == task.state).map(_.dest)).map {
      case n: Next => toNext(proc, task, curr)
      case p: Prev => toPrev(proc, task, curr)
      case Goto(s) =>
        proc.step(s).map(n => mv(proc, task, curr, n, overrideStrict = true)).getOrElse {
          Left(NotFound(s"Could not find goto step $s for ${task.id}"))
        }
    }.getOrElse(fallback(curr))
  }

  /**
   * Will calculate the next step in the process and move the Task accordingly.
   *
   * @param p the Process
   * @param t the Task to move
   * @return HIPEResult with the updated Task
   */
  protected def moveToNext(p: Process, t: Task): HIPEResult[Task] = transition(p, t)(curr => toNext(p, t, curr))

  /**
   * Will calculate the previous step in the process and move the Task accordingly.
   *
   * @param p the Process
   * @param t the Task to move
   * @return HIPEResult with the updated Task
   */
  protected def moveToPrevious(p: Process, t: Task): HIPEResult[Task] = transition(p, t)(prev => toPrev(p, t, prev))

  /**
   * This function allows for moving a Task through the Process. If in a strict
   * Process, the movement will be restricted to the previous and/or next step.
   * If it is open, the task can be moved anywhere. Even for open processes, if
   * the minCompleted assignments criteria is configured but not met, the move
   * will not be allowed.
   *
   * @param proc the Process to move the task within
   * @param newStepId The new StepId to move to
   * @return A HIPEResult of Task. Will have a Left value if the move was restricted.
   */
  protected def moveTask(proc: Process, task: Task, newStepId: StepId): HIPEResult[Task] =
    proc.step(newStepId).map(next => mv(proc, task, proc.step(task.stepId).get, next))

  /**
   * Will try to creates a new Task and "place" it in the first step of the process.
   * And initialise the steps configured minimum number of assignments.
   *
   * @param by the UserId creating the task
   * @param proc the Process
   * @param taskTitle The title of the task
   * @param taskDesc optional description text
   * @param due optional due-date
   * @return Option[Task]
   */
  protected def createTask(by: UserId, proc: Process, taskTitle: String, taskDesc: Option[String], due: Option[DateTime]): Option[Task] =
    for {
      step <- proc.stepGroups.headOption.flatMap(_.steps.headOption)
      pid <- proc.id
      sid <- step.id
    } yield {
      val version = VersionStamp(created = Some(UserStamp.create(by)))
      val t = Task(
        v = Some(version),
        id = TaskId.createOpt(),
        processId = pid,
        stepId = sid,
        title = taskTitle,
        description = taskDesc,
        dueDate = due,
        state = TaskStates.Open()
      )
      t.prepareFor(step)
    }

  /**
   * Will try to creates a new Task and "place" it in the first step of the process.
   * And initialise the steps configured minimum number of assignments.
   *
   * @param by the UserId creating the task
   * @param proc the Process
   * @param task the Task to base initialisation on
   * @return Option[Task]
   */
  protected def createTask(by: UserId, proc: Process, task: Task): Option[Task] =
    for {
      step <- proc.stepGroups.headOption.flatMap(_.steps.headOption)
      pid <- proc.id
      sid <- step.id
    } yield {
      val version = VersionStamp(created = Some(UserStamp.create(by)))
      val t = task.copy(
        v = Some(version),
        id = TaskId.createOpt(),
        processId = pid,
        stepId = sid,
        state = TaskStates.Open()
      )
      t.prepareFor(step)
    }

  /**
   * Will attempt to assign the given userId to an assignment on the provided Task.
   *
   * @param task the Task give an assignment on
   * @param assignTo the userId to assign
   * @return Some Task if an assignment was given to the user, otherwise None.
   */
  protected def assign(task: Task, assignTo: UserId): Option[Task] =
    task.assignmentApply(
      cond = t => !t.assignments.exists(_.assignee.contains(assignTo)),
      cp = _.filterNot(_.completed).find(_.assignee.isEmpty).map { a =>
        val aa: Assignment = a.copy(assignee = Some(assignTo))
        aa.assignmentStateApply(AssignmentStates.Assigned())
      }
    )

  protected def newAssignment(proc: Process, task: Task): Option[Task] =
    proc.stepGroups.flatten.findStep(task.stepId).map(s => task.addAssignmentFor(s))

  /**
   * Attempts to mark a users assignment as completed.
   *
   * @param assignee the userId that potentially has an asignment
   * @param task the Task where the assignment is located
   * @return the updated Task or None
   */
  protected def completeAssignment(assignee: UserId, task: Task): Option[Task] =
    task.assignmentApply(
      cond = _.assignments.exists(_.assignee.contains(assignee)),
      cp = _.filterNot(_.completed).find(_.assignee.contains(assignee)).map { a =>
        a.assignmentStateApply(AssignmentStates.Completed())
      }
    )

  /**
   * Force aborts any Assignments in the collection
   *
   * @param assignments the Assignments to abort
   * @return the list with all Assignments aborted or completed
   */
  private[this] def abortIncompleteAssignments(assignments: Seq[Assignment]): Seq[Assignment] =
    assignments.map(a =>
      a.status match {
        case AssignmentStates.Aborted() => a
        case AssignmentStates.Completed() => a
        case _ => a.assignmentStateApply(AssignmentStates.Aborted())
      }
    )

  /**
   * Convenience function for moving the Task to the next step. Marks the
   * task state as "Approved"
   *
   * @param proc the Process
   * @param task the Task to approve
   * @return HIPEResult with the new Task
   */
  protected def approve(proc: Process, task: Task): HIPEResult[Task] =
    moveToNext(proc, task.copy(state = TaskStates.Approved()))

  /**
   * Convenience function for moving the Task to the next step. Marks the
   * task state as "Consolidated"
   *
   * @param proc the Process
   * @param task the Task to consolidate
   * @return HIPEResult with the new Task
   */
  protected def consolidate(proc: Process, task: Task): HIPEResult[Task] = {
    val aborted = abortIncompleteAssignments(task.assignments)
    moveToNext(proc, task.copy(assignments = aborted, state = TaskStates.Consolidated()))
  }

  /**
   * Convenience function for moving the Task to the previous step. Marks the
   * task state as "Rejected"
   *
   * @param proc the Process
   * @param task the Task to reject
   * @return HIPEResult with the new Task
   */
  protected def reject(proc: Process, task: Task): HIPEResult[Task] = {
    val assigns = task.assignments.map(_.assignmentStateApply(AssignmentStates.Aborted()))
    moveToPrevious(proc, task.copy(state = TaskStates.Rejected(), assignments = assigns))
  }

  /**
   * Convenience function for moving the Task to the previous step. Marks the
   * task state as "Declined"
   *
   * @param proc the Process
   * @param task the Task to decline
   * @return HIPEResult with the new Task
   */
  protected def decline(proc: Process, task: Task): HIPEResult[Task] = {
    val assigns = task.assignments.map(_.assignmentStateApply(AssignmentStates.Aborted()))
    moveToPrevious(proc, task.copy(state = TaskStates.Declined(), assignments = assigns))
  }

}