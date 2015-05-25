/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations._
import hipe.core.AssignmentDetails.Assignment
import hipe.core._
import models.parties.UserId
import org.specs2._
import org.specs2.matcher.MatchResult
import org.specs2.specification.mutable.SpecificationFeatures

class TaskOperationsSpec extends mutable.Specification with TaskOperations with ProcessTestData with TaskValidators {

  "Tasks in a strict Process" should {
    var t: Option[Task] = None

    def failMove(stepId: StepId): MatchResult[Any] = {
      moveTask(strictProcess, t.get, stepId).isEmpty must_== true
    }

    def testAssign(uid: UserId, numAssignments: Int, idx: Int): MatchResult[Any] = {
      t = Option(assign(t.get, uid))
      t.isDefined must_== true
      assertAssignments(t.get.assignments, numAssignments)
      assertAssignment(t.get.assignments(idx), Some(uid))
    }

    def testComplete(uid: UserId, numAssignments: Int, idx: Int): MatchResult[Any] = {
      t = Option(completeAssignment(t.get, uid))
      t.isDefined must_== true
      assertAssignments(t.get.assignments, numAssignments)
      assertAssignment(t.get.assignments(idx), Some(uid), c = true)
    }

    def testMoveTask(sid: StepId, numAssignments: Int): MatchResult[Any] = {
      t = moveTask(strictProcess, t.get, sid)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== sid
      assertAssignments(t.get.assignments, numAssignments)
    }

    "be added in the first Step and generate 1 assignment" in {
      t = createTask(strictProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId0
      assertAssignments(t.get.assignments, 1)
    }

    "allow a user to be assigned to the assignment" in testAssign(uid0, 1, 0)

    "not allow a user to be assigned to the claimed assignment" in {
      t = Option(assign(t.get, uid1))
      t.isDefined must_== true
      assertAssignments(t.get.assignments, 1)
      t.get.assignments.head.assignee.get must_!= uid1
    }

    "allow a user to complete an assignment" in testComplete(uid0, 1, 0)

    "fail when moving beyond next Step" in failMove(stepId3)

    "move to second Step and generate 2 assignments" in testMoveTask(stepId1, 2)

    "allow users to be assigned to both assignments" in {
      testAssign(uid0, 2, 0)
      testAssign(uid1, 2, 1)
    }

    "fail when moving to third step before assignments are complete" in failMove(stepId2)

    "move to third Step and generate 2 new assignments when 1 assignment is completed" in {
      testComplete(uid1, 2, 1)
      testMoveTask(stepId2, 2)
    }

    "assign both assignments for third step" in {
      testAssign(uid0, 2, 0)
      testAssign(uid1, 2, 1)
    }

    "complete first assignment for third step" in testComplete(uid0, 2, 0)

    "fail when moving to fourth step before both assignments are complete" in failMove(stepId3)

    "complete second assignment for third step" in testComplete(uid1, 2, 1)

    "fail when moving back past previous Step" in failMove(stepId0)

    "move to fourth Step and generate 1 new assignment" in testMoveTask(stepId3, 1)

    "assign and complete the assignment for the fourth step" in {
      testAssign(uid2, 1, 0)
      testComplete(uid2, 1, 0)
    }

    "move to previous Step" in testMoveTask(stepId2, 2)

  }

  "Tasks in a non-strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = createTask(openProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId0
    }
    "move beyond next Step" in {
      t.isDefined must_== true
      t = moveTask(openProcess, t.get, stepId3)
      t.isDefined must_== true
    }
    "move back past previous Step" in {
      t.isDefined must_== true
      t = moveTask(openProcess, t.get, stepId0)
      t.isDefined must_== true
    }
    "move to next Step" in {
      t.isDefined must_== true
      t = moveTask(openProcess, t.get, stepId1)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId1
    }
    "move to next Step again" in {
      t.isDefined must_== true
      t = moveTask(openProcess, t.get, stepId2)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId2
    }
    "move to previous Step" in {
      t.isDefined must_== true
      t = moveTask(openProcess, t.get, stepId3)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId3
    }
  }

}

trait TaskValidators extends SpecificationFeatures {

  def assertAssignments(a: Seq[Assignment], expectedSize: Int): MatchResult[Any] = {
    a.nonEmpty must_== true
    a.size must_== expectedSize
  }

  def assertAssignment(actual: Assignment, expected: Assignment): MatchResult[Any] = {
    actual.assignedDate must_!= None
    actual.assignee must_== expected.assignee
    actual.completed must_== expected.completed
    if (expected.completed) {
      actual.completionDate must_!= None
    } else {
      actual.completionDate must_== None
    }
  }

  def assertAssignment(actual: Assignment, u: Option[UserId] = None, c: Boolean = false): MatchResult[Any] = {
    assertAssignment(actual, Assignment(assignee = u, completed = c))
  }

}