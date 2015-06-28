/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import akka.actor.ActorSystem
import hipe.HIPEService.TaskService
import hipe.core.States.{AssignmentState, AssignmentStates, TaskState, TaskStates}
import hipe.core.{Process, StepId, Task}
import models.parties.UserId
import org.specs2.matcher.MatchResult
import org.specs2.mutable
import org.specs2.specification.mutable.SpecificationFeatures
import test.util.mongodb.MongoSpec

class TaskServiceSpec extends mutable.Specification with MongoSpec with TaskServiceTesters with ProcessTestData {

  sequential

  // Need to save the process for certain services to function correctly
  Process.save(strictProcess)

  val title = "foo2"
  val desc = Some("bar2")
  implicit val ts = TestState[Task]()

  "When using the TaskService it" should {

    "be possible to create a new Task by using a Task argument" in {
      val t = Task(
        processId = strictProcess.id.get,
        stepId = strictProcess.stepGroups.flatten.head.id.get,
        title = "foo1",
        description = Some("bar1"),
        state = TaskStates.Open()
      )
      val maybeRes = taskService.create(uid0, strictProcess, t)
      assertTaskCreated(maybeRes, "foo1", Some("bar1"))
    }
    "be possible to create a new Task using title and description arguments" in {
      ts.t = taskService.create(uid0, strictProcess, title, desc)
      assertTaskCreated(ts.t, title, desc)
    }
    "be possible to change the title of a Task" in {
      val orig = ts.t.get
      val updTitle = "Foo 2.2"
      ts.t = taskService.changeTitle(uid1, orig.id.get, updTitle)
      assertTaskUpdated(ts.t, orig, updTitle, orig.description, TaskStates.Open())
    }
    "be possible to change the description of a Task" in {
      val orig = ts.t.get
      val updDesc = Some("Bar 2.2")
      ts.t = taskService.changeDescription(uid1, orig.id.get, updDesc)
      assertTaskUpdated(ts.t, orig, orig.title, updDesc, TaskStates.Open())
    }
    "be possible to assign a Task Assignment" in {
      val orig = ts.t.get
      ts.t = taskService.assignTo(uid1, orig.id.get, uid0)
      assertAssignmentUpdate(ts.t, orig, uid0, AssignmentStates.Assigned())
    }
    "be possible to complete a Task Assignment" in {
      val orig = ts.t.get
      ts.t = taskService.complete(uid0, orig.id.get)
      assertAssignmentUpdate(ts.t, orig, uid0, AssignmentStates.Completed())
    }
    "be possible to approve a Task and move it to the next Step in the Process" in {
      val orig = ts.t.get
      ts.t = taskService.approveTask(uid0, orig.id.get)
      assertTaskApproved(ts.t, orig, stepId1, 2)
    }
    "be possible to delegate a Task Assignment that is already claimed" in {
      val orig = ts.t.get
      // First...give an assignment to a user
      val a = taskService.assignTo(uid1, orig.id.get, uid0)
      assertAssignmentUpdate(a, orig, uid0, AssignmentStates.Assigned())
      // Then delegate it
      ts.t = taskService.delegateTo(uid0, orig.id.get, uid2)
      assertAssignmentUpdate(ts.t, a.get, uid2, AssignmentStates.Assigned())
    }
    "be possible to move a Task to any StepId in the Process" in {
      pending("Requires using an 'open' process...")
    }
    "be possible to reject a Task" in {
      val orig = ts.t.get
      ts.t = taskService.rejectTask(uid0, orig.id.get)
      assertTaskRejected(ts.t, orig, stepId0, 1)
    }
    "be possible to locate the latest version of a Task by TaskId" in {
      val actual = taskService.findById(ts.t.get.id.get)
      actual must_== ts.t
    }
    "be possible to locate full revision of a Task from the journal" in {
      todo
    }
    "be possible to locate all Tasks for a given ProjectId" in {
      val all = taskService.findByProcessId(ts.t.get.processId)
      all.size must_== 2
    }
  }

}

trait TaskServiceTesters extends SpecificationFeatures with ProcessTestData {

  val taskService = new TaskService(ActorSystem("test-hipe-system"))

  def defaultTaskAsserts(maybeRes: Option[Task]): MatchResult[Any] = {
    maybeRes must_!= None
    maybeRes.get.v must_!= None
    maybeRes.get.id must_!= None
  }

  def assertTaskCreated(maybeRes: Option[Task], expTitle: String, expDesc: Option[String]): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val res = maybeRes.get
    res.stepId must_== stepId0
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

  def assertAssignmentUpdate(maybeRes: Option[Task], orig: Task, expAssignee: UserId, expTaskState: AssignmentState): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.id must_== orig.id
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.assignments.size must_== orig.assignments.size
    r.assignments.exists(a => a.assignee.contains(expAssignee) && a.status == expTaskState) must_== true
  }

  def assertTaskMove(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] = {
    defaultTaskAsserts(maybeRes)
    val r = maybeRes.get
    r.v must_!= orig.v
    r.v.get.version must_== orig.v.get.version + 1
    r.stepId must_== expStepId
    r.assignments.size must_== expAssignments
  }

  def assertTaskApproved(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] = {
    assertTaskMove(maybeRes, orig, expStepId, expAssignments)
    maybeRes.get.state must_== TaskStates.Approved()
  }

  def assertTaskRejected(maybeRes: Option[Task], orig: Task, expStepId: StepId, expAssignments: Int): MatchResult[Any] = {
    assertTaskMove(maybeRes, orig, expStepId, expAssignments)
    maybeRes.get.state must_== TaskStates.Rejected()
  }

}