/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEService.TaskService
import hipe.core.States.TaskStates
import hipe.core.Task
import org.specs2.mutable
import test.util.mongodb.MongoSpec

class TaskServiceSpec extends mutable.Specification with MongoSpec with ProcessTestData {

  sequential

  "When using the TaskService it" should {

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
    "be possible to create a new Task" in {
      val title = "foo2"
      val desc = Some("bar2")
      var maybeTask: Option[Task] = None

      "using title and description arguments" in {
        maybeTask = TaskService.create(uid0, strictProcess, title, desc)
        maybeTask must_!= None

        val r1 = maybeTask.get
        r1.v must_!= None
        r1.id must_!= None
        r1.title must_== "foo2"
        r1.description.contains("bar2") must_== true
        r1.assignments.size must_!= 0
      }
      "and add a new version when modified" in {
        val orig = maybeTask.get
        val t = orig.copy(description = Some("bar2.2"))
        maybeTask = TaskService.update(uid1, orig.id.get, t)

        //        println(Json.prettyPrint(Json.toJson(orig)))
        //        println(Json.prettyPrint(Json.toJson(maybeTask.get)))

        maybeTask must_!= None
        val r2 = maybeTask.get
        r2.v must_!= None
        r2.v must_!= orig.v
        r2.id must_!= None
        r2.id must_== orig.id
        r2.description.get must_!= orig.description.get
      }
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