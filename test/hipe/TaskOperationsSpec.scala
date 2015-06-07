/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations.TaskOperations
import hipe.core.States.AssignmentStates._
import hipe.core.States.{AssignmentState, TaskState, TaskStates}
import hipe.core._
import models.parties.UserId
import org.specs2._
import org.specs2.matcher.MatchResult
import org.specs2.specification.mutable.SpecificationFeatures

class TaskOperationsSpec extends mutable.Specification with TaskOperations with ProcessTestData with TaskValidators {

  "Tasks in a strict Process" should {
    var t: Option[Task] = None

    def failMoveTo(stepId: StepId): MatchResult[Any] = moveTask(strictProcess, t.get, stepId).isEmpty must_== true

    def failMoveNext: MatchResult[Any] = moveToNext(strictProcess, t.get).isEmpty must_== true

    def failMovePrev: MatchResult[Any] = moveToPrevious(strictProcess, t.get).isEmpty must_== true

    def testAssign(uid: UserId, numAssignments: Int, idx: Int): MatchResult[Any] = {
      t = assign(t.get, uid)
      t.isDefined must_== true
      assertAssignments(t.get.assignments, numAssignments)
      assertAssignment(t.get.assignments(idx), Some(uid), s = Assigned())
    }

    def testComplete(uid: UserId, numAssignments: Int, idx: Int): MatchResult[Any] = {
      t = completeAssignment(t.get, uid)
      t.isDefined must_== true
      assertAssignments(t.get.assignments, numAssignments)
      assertAssignment(t.get.assignments(idx), Some(uid), s = Completed())
    }

    def testApprove(expectedStep: StepId, expectedAssignments: Int): MatchResult[Any] =
      testMoveTask(expectedStep, expectedAssignments, TaskStates.Approved())(approve)

    def testReject(expectedStep: StepId, expectedAssignments: Int): MatchResult[Any] =
      testMoveTask(expectedStep, expectedAssignments, TaskStates.Rejected())(reject)

    def testMoveTask(targetStep: StepId, expectedAssignments: Int, expectedState: TaskState)(move: (Process, Task) => HIPEResult[Task]): MatchResult[Any] = {
      t = move(strictProcess, t.get)
      validateTaskMove(targetStep, expectedAssignments, expectedState)
    }

    def validateTaskMove(targetStep: StepId, expectedAssignments: Int, expectedState: TaskState): MatchResult[Any] = {
      commonTaskAssert(t, strictProcess, targetStep, expectedState)
      assertAssignments(t.get.assignments, expectedAssignments)
    }

    "be added in the first Step and generate 1 assignment" in {
      t = createTask(uid0, strictProcess, "card 1", None)
      commonTaskAssert(t, strictProcess, stepId0, TaskStates.Open())
      assertAssignments(t.get.assignments, 1)
    }

    "allow a user to be assigned to the assignment" in testAssign(uid0, 1, 0)

    "not allow a user to be assigned to the claimed assignment" in {
      t = assign(t.get, uid1)
      t.isDefined must_== true
      assertAssignments(t.get.assignments, 1)
      t.get.assignments.head.assignee.get must_!= uid1
    }

    "allow a user to complete an assignment" in testComplete(uid0, 1, 0)

    "fail when moving beyond next Step" in failMoveTo(stepId3)

    "move to second Step and generate 2 assignments" in testApprove(stepId1, 2)

    "allow users to be assigned to both assignments" in {
      testAssign(uid0, 2, 0)
      testAssign(uid1, 2, 1)
    }

    "fail when moving to third step before assignments are complete" in failMoveNext

    "move to third Step and generate 2 new assignments when 1 assignment is completed" in {
      testComplete(uid1, 2, 1)
      testApprove(stepId2, 2)
    }

    "assign both assignments for third step" in {
      testAssign(uid0, 2, 0)
      testAssign(uid1, 2, 1)
    }

    "complete first assignment for third step" in testComplete(uid0, 2, 0)

    "fail when moving to fourth step before both assignments are complete" in failMoveNext

    "fail when moving back to third step before both assignments are complete" in failMovePrev

    "complete second assignment for third step" in testComplete(uid1, 2, 1)

    "fail when moving back past previous Step" in failMoveTo(stepId0)

    "move to fourth Step and generate 1 new assignment" in testApprove(stepId3, 1)

    "send the task back from the fourth Step by rejecting the Task" in testReject(stepId2, 2)

    "complete the third Step and move to the fourth Step again" in {
      testAssign(uid0, 2, 0)
      testComplete(uid0, 2, 0)
      testAssign(uid1, 2, 1)
      testComplete(uid1, 2, 1)
      testApprove(stepId3, 1)
    }

    "assign and complete the assignment for the fourth step" in {
      testAssign(uid2, 1, 0)
      testComplete(uid2, 1, 0)
    }

    "move to previous Step when task is complete" in testReject(stepId2, 2)

  }

  "Tasks in a non-strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = createTask(uid0, openProcess, "card 1", None)
      commonTaskAssert(t, openProcess, stepId0, TaskStates.Open())
    }
    "move beyond next Step" in {
      t = moveTask(openProcess, t.get, stepId3)
      commonTaskAssert(t, openProcess, stepId3, TaskStates.Open())
    }
    "move back past previous Step" in {
      t = moveTask(openProcess, t.get, stepId0)
      commonTaskAssert(t, openProcess, stepId0, TaskStates.Open())
    }
    "move to next Step" in {
      t = moveToNext(openProcess, t.get)
      commonTaskAssert(t, openProcess, stepId1, TaskStates.Open())
    }
    "move to next Step again" in {
      t = moveToNext(openProcess, t.get)
      commonTaskAssert(t, openProcess, stepId2, TaskStates.Open())
    }
    "move to previous Step" in {
      t = moveToPrevious(openProcess, t.get)
      commonTaskAssert(t, openProcess, stepId1, TaskStates.Open())
    }
  }

}

trait TaskValidators extends SpecificationFeatures {

  def commonTaskAssert(t: Option[Task], proc: Process, expStepId: StepId, expState: TaskState): MatchResult[Any] = {
    t.isDefined must_== true
    t.get.processId must_== proc.id.get
    t.get.stepId must_== expStepId
    t.get.state must_== expState
  }

  def assertAssignments(a: Seq[Assignment], expectedSize: Int): MatchResult[Any] = {
    a.nonEmpty must_== true
    a.size must_== expectedSize
  }

  def assertAssignment(actual: Assignment, expected: Assignment): MatchResult[Any] = {
    actual.assignedDate must_!= None
    actual.assignee must_== expected.assignee
    actual.status must_== expected.status
    if (expected.completed) {
      actual.completionDate must_!= None
    } else {
      actual.completionDate must_== None
    }
  }

  def assertAssignment(actual: Assignment, u: Option[UserId] = None, s: AssignmentState = Available()): MatchResult[Any] = {
    assertAssignment(actual, Assignment(assignee = u, status = s))
  }

}