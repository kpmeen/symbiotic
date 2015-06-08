/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEService.TaskService
import hipe.core.States.{AssignmentStates, TaskStates}
import hipe.core.Task
import org.specs2.mutable
import play.api.libs.json.Json
import test.util.mongodb.MongoSpec

class TaskServiceSpec extends mutable.Specification with MongoSpec with ProcessTestData {

  sequential

  "When using the TaskService it" should {

    val title = "foo2"
    val desc = Some("bar2")
    var maybeTask: Option[Task] = None

    "be possible to create a new Task by using a Task argument" in {
      val t = Task(
        processId = strictProcess.id.get,
        stepId = strictProcess.stepGroups.flatten.head.id.get,
        title = "foo1",
        description = Some("bar1"),
        state = TaskStates.Open()
      )
      val maybeRes = TaskService.create(uid0, strictProcess, t)
      maybeRes must_!= None

      val res = maybeRes.get
      res.v must_!= None
      res.id must_!= None
      res.title must_== "foo1"
      res.description.contains("bar1") must_== true
      res.assignments.size must_!= 0
    }
    "be possible to create a new Task using title and description arguments" in {
      maybeTask = TaskService.create(uid0, strictProcess, title, desc)
      maybeTask must_!= None

      val r1 = maybeTask.get
      r1.v must_!= None
      r1.id must_!= None
      r1.title must_== "foo2"
      r1.description.contains("bar2") must_== true
      r1.assignments.size must_!= 0
    }
    "add a new version when a Task is modified" in {
      val orig = maybeTask.get
      val t = orig.copy(description = Some("bar2.2"))
      maybeTask = TaskService.update(uid1, orig.id.get, t)

      maybeTask must_!= None
      val r2 = maybeTask.get
      r2.v must_!= None
      r2.v must_!= orig.v
      r2.id must_!= None
      r2.id must_== orig.id
      r2.description.get must_!= orig.description.get
      r2.assignments.size must_== orig.assignments.size
    }
    "be possible to assign a Task" in {
      val orig = maybeTask.get

      maybeTask = TaskService.assignTo(uid1, orig.id.get, uid0)

      maybeTask must_!= None
      val r3 = maybeTask.get
      r3.v must_!= None
      r3.v.get must_!= orig.v.get
      r3.v.get.version must_== orig.v.get.version + 1
      r3.assignments.size must_== orig.assignments.size
      r3.assignments.exists(a => a.assignee.contains(uid0) && a.status == AssignmentStates.Assigned()) must_== true
    }
    "be possible to complete a Task" in {
      val orig = maybeTask.get

      maybeTask = TaskService.complete(uid0, orig.id.get)

      maybeTask must_!= None
      val r4 = maybeTask.get
      r4.v must_!= None
      r4.v.get must_!= orig.v.get
      r4.v.get.version must_== orig.v.get.version + 1
      r4.assignments.size must_== orig.assignments.size
      r4.assignments.exists(a => a.assignee.contains(uid0) && a.status == AssignmentStates.Completed()) must_== true
    }
    "be possible to reject a Task" in {
      todo
    }
    "be possible to delegate a Task" in {
      todo
    }
    "be possible to move a Task to a StepId in the Process" in {
      todo
    }
    "be possible to move a Task to the next Step in the Process" in {
      todo
    }
    "be possible to move a Task to the previous Step in the Process" in {
      todo
    }
    "be possible to locate a Task by TaskId" in {
      todo
    }
    "be possible to locate all versions of a Task by TaskId" in {
      todo
    }
    "be possible to locate all Tasks for a given ProjectId" in {
      todo
    }
  }

}