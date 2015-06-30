/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import akka.actor.ActorSystem
import hipe.HIPEService.TaskService
import hipe.core.States.{AssignmentState, TaskState, TaskStates}
import hipe.core.{StepId, Task}
import models.parties.UserId
import org.specs2.matcher.MatchResult
import org.specs2.specification.mutable.SpecificationFeatures

trait DummyTaskService {
  val taskService = new TaskService(ActorSystem("test-hipe-system"))
}

trait TaskServiceTesters extends SpecificationFeatures {

  def defaultTaskAsserts(maybeRes: Option[Task]): MatchResult[Any] = {
    maybeRes must_!= None
    maybeRes.get.v must_!= None
    maybeRes.get.id must_!= None
  }

  def assertTaskCreated(maybeRes: Option[Task], expTitle: String, expDesc: Option[String], expStepId: StepId): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val res = maybeRes.get
    res.stepId must_== expStepId
    res.title must_== expTitle
    res.description must_== expDesc
    res.assignments.size must_!= 0
    res.state must_== TaskStates.Open()
  }

  def assertTaskUpdated(maybeRes: Option[Task], orig: Task, expTitle: String, expDesc: Option[String], expState: TaskState): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.id must_== orig.id
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.title must_== expTitle
    r.description must_== expDesc
    r.assignments.size must_== orig.assignments.size
  }

  def assertTaskConsolidated(maybeRes: Option[Task], orig: Task, expStepId: StepId, expState: TaskState, expAssignments: Int): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.id must_== orig.id
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.title must_== orig.title
    r.description must_== orig.description
    r.stepId must_== expStepId
    r.assignments.size must_== expAssignments
  }

  def assertAssignmentUpdate(maybeRes: Option[Task], orig: Task, expAssignee: UserId, expTaskState: AssignmentState): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.id must_== orig.id
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.assignments.size must_== orig.assignments.size
    r.assignments.exists(a => a.assignee.contains(expAssignee) && a.status == expTaskState) must_== true
  }

  def assertAssignmentAdded(maybeRes: Option[Task], orig: Task, expAssignments: Int): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.stepId must_== orig.stepId
    r.assignments.size must_== expAssignments
  }

  def assertTaskMove(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.stepId must_== expStepId
    r.assignments.size must_== expAssignments
  }

  def assertState(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int, expState: TaskState): MatchResult[Any] = {
    assertTaskMove(maybeRes, orig, expStepId, expAssignments)
    maybeRes.get.state must_== expState
  }

  def assertTaskApproved(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] =
    assertState(maybeRes, orig, expStepId, expAssignments, TaskStates.Approved())

  def assertTaskNotApproved(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] =
    assertState(maybeRes, orig, expStepId, expAssignments, TaskStates.Declined())

  def assertTaskRejected(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] =
    assertState(maybeRes, orig, expStepId, expAssignments, TaskStates.Rejected())

}