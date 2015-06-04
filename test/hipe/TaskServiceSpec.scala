/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEService.TaskService
import hipe.core.States.TaskStates
import hipe.core.Task
import org.specs2.mutable
import test.util.mongodb.MongoSpec

class TaskServiceSpec  extends mutable.Specification with MongoSpec with ProcessTestData {

  "When using the TaskService it" should {

    "be possible to create a new Task by using a Task argument" in {
      val t = Task(
        processId = strictProcess.id.get,
        stepId = strictProcess.stepGroups.flatten.head.id.get,
        title = "foo1",
        description = Some("bar1"),
        state = TaskStates.Open()
      )
      val maybeRes = TaskService.create(strictProcess, t)
      maybeRes must_!= None

      val res = maybeRes.get
      res.assignments.size must_!= 0
      res.id must_!= None
      res.title must_== "foo1"
      res.description.contains("bar1") must_== true
    }
    "be possible to create a new Task using title and description arguments" in {
      val title = "foo2"
      val desc = Some("bar2")

      val maybeRes = TaskService.create(strictProcess, title, desc)
      maybeRes must_!= None

      val res = maybeRes.get
      res.assignments.size must_!= 0
      res.id must_!= None
      res.title must_== "foo2"
      res.description.contains("bar2") must_== true
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
    "be possible to add a new version (a.k.a. update) of a Task" in {
      todo
    }
    "be possible to complete a Task" in {
      todo
    }
    "be possible to reject a Task" in {
      todo
    }
    "be possible to delegate a Task" in {
      todo
    }
    "be possible to assign a Task" in {
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
  }

}
