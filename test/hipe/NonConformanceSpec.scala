/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import akka.actor.ActorSystem
import hipe.HIPEService.{ProcessService, TaskService}
import hipe.core._
import models.parties.UserId
import org.specs2.mutable
import test.util.mongodb.MongoSpec

/**
 * Test scenarios setting up and going through a Non-Conformance process.
 */
class NonConformanceSpec extends mutable.Specification with MongoSpec with NCRTestHelpers with NCRProcessTestData {

  sequential


  implicit val ps = TestState[Process]()
  implicit val ts = TestState[Task]()

  "Creating a NCR process" should {
    "Add a new process" in {
      val res = processService.create(name = "NCR Proc", strict = true, desc = Some("Process for reviewing NCR's"))
      ps.t = Some(res)
      res.name must_== "NCR Proc"
      res.description must_== Some("Process for reviewing NCR's")
      res.strict must_== true
    }
    "Add step for evaluating NCR" in {
      val curr = ps.t.get
      val res = processService.addStep(curr.id.get, step1)
      res.isRight must_== true
      val p = res.right.get
      ps.t = Some(p)
      p.stepGroups.size must_== 1
    }
    "Add private step group for internal review of NCR" in {
      val curr = ps.t.get
      val res = processService.addGroup(curr.id.get, StepGroup(StepGroupId.createOpt(), Some("Review"), priv = true))
      res.isRight must_== true
      val p = res.right.get
      ps.t = Some(p)
      p.stepGroups.size must_== 2
    }
    "Add step to private group for internal review of the NCR" in {
      val curr = ps.t.get
      val res = processService.addStepToGroup(curr.id.get, curr.stepGroups.last.id.get, step2)
      res.isRight must_== true
      val p = res.right.get
      ps.t = Some(p)
      p.stepGroups.size must_== 2
      p.stepGroups.flatten.size must_== 2
    }
    "Add step to the same private group for approving the NCR" in {
      val curr = ps.t.get
      val res = processService.addStepToGroup(curr.id.get, curr.stepGroups.last.id.get, step3)
      res.isRight must_== true
      val p = res.right.get
      ps.t = Some(p)
      p.stepGroups.size must_== 2
      p.stepGroups.flatten.size must_== 3
    }
    "Add step to indicate that an answer is provided for the NCR" in {
      val curr = ps.t.get
      val res = processService.addStep(curr.id.get, step4)
      res.isRight must_== true
      val p = res.right.get
      ps.t = Some(p)
      p.stepGroups.size must_== 3
      p.stepGroups.flatten.size must_== 4
    }
  }

  "Running through NCR's processes" should {
    "Place a new Task in the evaluate step and generate assignment(s)" in {
      todo
    }
    "Allow the NCR-coordinator to REJECT the request" in {
      todo
    }
    "Move the REJECTED task to the 'answer provided' step (generate no assignments)" in {
      todo
    }
    "Allow the NCR-coordinator to ACCEPT the request" in {
      todo
    }
    "Move the ACCEPTED tasks to the review/comment step and generate assignment(s)" in {
      todo
    }
    "Allow a user to COMPLETE a review/comment assignment" in {
      todo
    }
    "Be possible to add additional reviewers by generating more assignments" in {
      todo
    }
    "Be possible to CONSOLIDATE the review/comment task BEFORE the required number of assignments are completed" in {
      todo
    }
    "Be possible to CONSOLIDATE the review/comment task AFTER the required number of assignments are completed" in {
      todo
    }
    "Move the CONSOLIDATED review/comment task to the approval step and generate assignment(s)" in {
      todo
    }
    "Allow a user to RE-REVIEW the CONSOLIDATED task" in {
      todo
    }
    "Move the RE-REVIEW task to the 'review/comment' step and generate assignment(s)" in {
      todo
    }
    "Allow a user to REJECT the CONSOLIDATED task" in {
      todo
    }
    "Move the REJECTED task to the 'answer provided' step and (generate no assignments)" in {
      todo
    }
    "Allow a user to APPROVE the CONSOLIDATED task" in {
      todo
    }
    "Move the APPROVED task to the 'answer provided' step (generate no assignments)" in {
      todo
    }

  }

}

trait NCRTestHelpers {
  val as = ActorSystem("test-hipe-system")
  val taskService = new TaskService(as)
  val processService = new ProcessService(as, taskService)
}

trait NCRProcessTestData {
  val uid1 = UserId.create()
  val uid2 = UserId.create()
  val uid3 = UserId.create()

  val pid = ProcessId.create()

  val stepId1 = StepId.create()
  val stepId2 = StepId.create()
  val stepId3 = StepId.create()
  val stepId4 = StepId.create()

  val sgId1 = StepGroupId.create()
  val sgId2 = StepGroupId.create()
  val sgId3 = StepGroupId.create()

  val step1 = Step(
    id = Some(stepId1),
    name = "Evaluate",
    description = Some("Evalute the NCR"),
    minAssignments = 1, minCompleted = 1
  )
  val step2 = Step(
    id = Some(stepId2),
    name = "Review",
    description = Some("Review and Comment the NCR"),
    minAssignments = 3, minCompleted = 3
  )
  val step3 = Step(
    id = Some(stepId3),
    name = "Approval",
    description = Some("Approval of the NCR after reviews/comments"),
    minAssignments = 1, minCompleted = 1
  )
  val step4 = Step(
    id = Some(stepId4),
    name = "Done",
    description = Some("All done amigo"),
    minAssignments = 0, minCompleted = 0
  )

  val sg1 = StepGroup(id = Some(sgId1), name = Some("Evaluate"), steps = StepList(step1))
  val sg2 = StepGroup(id = Some(sgId2), name = Some("For Approval"), steps = StepList(step2, step3), priv = true)
  val sg3 = StepGroup(id = Some(sgId3), name = Some("Done"), steps = StepList(step4))

  val sgList = StepGroupList(sg1, sg2, sg3)

  val proc = Process(
    id = Some(pid),
    name = "Non-Conformance Requests",
    strict = true,
    description = Some("Review and approval process for NCR's"),
    stepGroups = sgList
  )
}
