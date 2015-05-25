/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations._
import hipe.core._
import org.specs2._

class TaskOperationsSpec extends mutable.Specification with TaskOperations with ProcessTestData {

  "Tasks in a strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = createTask(strictProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId0
    }
    "move to next Step" in {
      t.isDefined must_== true
      t = moveTask(strictProcess, t.get, stepId1)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId1

      t.isDefined must_== true
      t = moveTask(strictProcess, t.get, stepId2)
      t.isDefined must_== true
      t = moveTask(strictProcess, t.get, stepId3)
      t.isDefined must_== true
    }
    "fail when moving back past previous Step" in {
      moveTask(strictProcess, t.get, stepId0).isEmpty must_== true
    }
    "move to previous Step" in {
      t.isDefined must_== true
      t = moveTask(strictProcess, t.get, stepId2)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId2
    }
    "fail when moving beyond next Step" in {
      t.isDefined must_== true
      t = moveTask(strictProcess, t.get, stepId1)
      moveTask(strictProcess, t.get, stepId3).isEmpty must_== true
    }
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
