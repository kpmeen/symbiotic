/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import akka.actor.ActorSystem
import hipe.HIPEOperations.Implicits._
import hipe.HIPEService.{ProcessService, TaskService}
import hipe.core.States.TaskStates.{NotApproved, Rejected}
import hipe.core.States.{AssignmentStates, TaskStates}
import hipe.core._
import hipe.core.dsl.Rules.TransitionRule
import hipe.core.dsl.TaskStateRule
import models.parties.UserId
import org.specs2.mutable
import util.mongodb.MongoSpec

/**
 * Test scenarios setting up and going through a Non-Conformance process.
 */
class NonConformanceSpec extends mutable.Specification
with MongoSpec
with NCRTestHelpers
with NCRProcessTestData
with TaskServiceTesters {

  sequential

  processService.create(proc)

  implicit val ps = TestState[Process]()
  implicit val ts = TestState[Task]()

  "Creating a NCR process" should {
    // Need to create the process from "scratch"...but reuse what I can from the test data configs.
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
      val expTitle = "This is a NCR"
      val expDesc = Some("For testing the process flow")
      ts.t = taskService.create(
        by = uid1,
        p = proc,
        title = expTitle,
        desc = expDesc
      )
      assertTaskCreated(ts.t, expTitle, expDesc, stepId1)
    }
    "Allow an NCR-coordinator to claim the 'evaluate' task" in {
      val orig = ts.t.get
      ts.t = taskService.assignTo(uid2, orig.id.get, uid2)
      assertAssignmentUpdate(ts.t, orig, uid2, AssignmentStates.Assigned())
    }
    "Allow the NCR-coordinator to REJECT the request, and move the task to the 'answer provided' step" in {
      val expTitle = "RejectME"
      val expDesc = Some("Testing rejection")
      val maybeCreated = taskService.create(
        by = uid1,
        p = proc,
        title = expTitle,
        desc = expDesc
      )
      assertTaskCreated(maybeCreated, expTitle, expDesc, stepId1)
      val orig = maybeCreated.get
      val mc = taskService.assignTo(uid2, orig.id.get, uid2)
      assertAssignmentUpdate(mc, orig, uid2, AssignmentStates.Assigned())
      val maybeRejected = taskService.rejectTask(uid2, orig.id.get)
      assertTaskRejected(maybeRejected, mc.get, stepId4, 0)
    }
    "Allow the NCR-coordinator to ACCEPT the request, and move the task to the 'review' step" in {
      val orig = ts.t.get
      val mComp = taskService.complete(uid2, orig.id.get)
      assertAssignmentUpdate(mComp, orig, uid2, AssignmentStates.Completed())
      ts.t = taskService.approveTask(uid2, orig.id.get)
      assertTaskApproved(ts.t, mComp.get, stepId2, 3)
    }
    "Allow a reviewer to claim and COMPLETE a 'review' assignment" in {
      val orig = ts.t.get
      val mc = taskService.assignTo(uid1, orig.id.get, uid1)
      assertAssignmentUpdate(mc, orig, uid1, AssignmentStates.Assigned())
      ts.t = taskService.complete(uid1, orig.id.get)
      assertAssignmentUpdate(ts.t, mc.get, uid1, AssignmentStates.Completed())
    }
    "Be possible to delegate the 'review' task to another user" in {
      val orig = ts.t.get
      val uidX = UserId.create()
      val mc = taskService.assignTo(uid2, orig.id.get, uid2)
      assertAssignmentUpdate(mc, orig, uid2, AssignmentStates.Assigned())
      ts.t = taskService.assignTo(uid2, orig.id.get, uidX)
      assertAssignmentUpdate(ts.t, mc.get, uidX, AssignmentStates.Assigned())
    }
    "Be possible to add additional reviewers" in {
      val orig = ts.t.get
      val mass = taskService.addAssignment(uid1, orig.id.get)
      assertAssignmentAdded(mass, orig, orig.assignments.size + 1)
    }
    "Be possible to CONSOLIDATE the review/comment task BEFORE the required number of assignments are completed" in {
      val expTitle = "RejectME"
      val expDesc = Some("Testing rejection")
      val maybeCreated = taskService.create(
        by = uid1,
        p = proc,
        title = expTitle,
        desc = expDesc
      )
      assertTaskCreated(maybeCreated, expTitle, expDesc, stepId1)
      val orig = maybeCreated.get
      val mc = taskService.assignTo(uid2, orig.id.get, uid2)
      assertAssignmentUpdate(mc, orig, uid2, AssignmentStates.Assigned())
      val mComp = taskService.complete(uid2, orig.id.get)
      assertAssignmentUpdate(mComp, mc.get, uid2, AssignmentStates.Completed())
      val ma = taskService.approveTask(uid1, orig.id.get)
      assertTaskApproved(ma, mComp.get, stepId2, 3)

      val mCons = taskService.consolidateTask(uid1, orig.id.get)
      assertTaskConsolidated(mCons, ma.get, stepId3, TaskStates.Consolidated(), 1)
    }
    "Be possible to CONSOLIDATE the review/comment task AFTER the required number of assignments are completed" in {
      val orig = ts.t.get
      taskService.assignTo(uid2, orig.id.get, uid2)
      taskService.assignTo(uid3, orig.id.get, uid3)

      taskService.complete(uid2, orig.id.get)
      val ma = taskService.complete(uid3, orig.id.get)

      ts.t = taskService.consolidateTask(uid4, orig.id.get)
      assertTaskConsolidated(ts.t, ma.get, stepId3, TaskStates.Consolidated(), 1)
    }
    "Allow an NCR-coordinator to send the CONSOLIDATED task back for a new round of review" in {
      val orig = ts.t.get
      val ma = taskService.assignTo(uid1, orig.id.get, uid1)
      assertAssignmentUpdate(ma, orig, uid1, AssignmentStates.Assigned())
      val mr = taskService.rejectTask(uid1, orig.id.get)
      assertTaskRejected(mr, ma.get, stepId2, 3)
      ts.t = taskService.consolidateTask(uid2, orig.id.get)
      assertTaskConsolidated(ts.t, mr.get, stepId3, TaskStates.Consolidated(), 1)
    }
    "Allow an NCR-coordinator to REJECT the NCR and move to final step" in {
      val orig = ts.t.get
      ts.t = taskService.rejectTask(uid3, orig.id.get, TaskStates.NotApproved())
      assertTaskNotApproved(ts.t, orig, stepId4, 0)
    }
    "Allow an NCR-coordinator to APPROVE the NCR and move to final step" in {
      val orig = ts.t.get
      val res = taskService.rejectTask(uid4, orig.id.get)
      res.nonEmpty must_== true
      val ma = taskService.assignTo(uid3, orig.id.get, uid3)
      assertAssignmentUpdate(ma, res.get, uid3, AssignmentStates.Assigned())
      val mc = taskService.complete(uid3, orig.id.get)
      assertAssignmentUpdate(mc, ma.get, uid3, AssignmentStates.Completed())
      ts.t = taskService.approveTask(uid3, orig.id.get)
      assertTaskApproved(ts.t, mc.get, stepId4, 0)
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
  val uid4 = UserId.create()

  val pid = ProcessId.create()

  val stepId1 = StepId.create()
  val stepId2 = StepId.create()
  val stepId3 = StepId.create()
  val stepId4 = StepId.create()

  println(s"stepId 1: $stepId1")
  println(s"stepId 2: $stepId2")
  println(s"stepId 3: $stepId3")
  println(s"stepId 4: $stepId4")

  val sgId1 = StepGroupId.create()
  val sgId2 = StepGroupId.create()
  val sgId3 = StepGroupId.create()

  val step1 = Step(
    id = Some(stepId1),
    name = "Evaluate",
    description = Some("Evalute the NCR"),
    minAssignments = 1, minCompleted = 1,
    transitionRules = Some(Seq(
      TaskStateRule(Rejected(), TransitionRule(s"when task is rejected go to step ${stepId4.value}"))
    ))
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
    minAssignments = 1, minCompleted = 1,
    transitionRules = Some(Seq(
      TaskStateRule(NotApproved(), TransitionRule(s"when task is not approved go to step ${stepId4.value}"))
    ))
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
