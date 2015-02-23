/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package workflow.engine

import com.mongodb.casbah.TypeImports.ObjectId
import org.scalatest._
import workflow.engine.Processes._
import workflow.engine.Steps._

class ProcessSpec extends WordSpec with MustMatchers {

  "A Process" should {
    var proc = Process(_id = ProcessId(new ObjectId()), name = "Test Process", description = Some("Just for testing purposes"))

    "be initialized with no steps" in {
      assert(proc.steps.isEmpty)
    }

    "add a Step to the empty steps list" in {
      proc = proc.appendStep(step0)
      assert(proc.steps.length == 1)
    }
    "append a Step to the steps list" in {
      proc = proc.appendStep(step2)
      assert(proc.steps.length == 2)
    }
    "insert a Step after the first Step" in {
      proc = proc.insertStep(step1, 1)
      assert(proc.steps.length == 3)
      assertResult(proc.steps.head.id)(step0.id)
      assertResult(proc.steps.last.id)(step2.id)
      assertResult(proc.steps(1).id)(step1.id)
    }
    "insert a Step before the second to last Step" in {
      proc = proc.insertStep(step3, 2)
      assert(proc.steps.length == 4)
      assertResult(proc.steps.head.id)(step0.id)
      assertResult(proc.steps(1).id)(step1.id)
      assertResult(proc.steps(2).id)(step3.id)
      assertResult(proc.steps.last.id)(step2.id)
    }
    "should move the second to last Step to the front" in {
      proc = proc.moveStep(2, 0)
      assert(proc.steps.length == 4)
      assertResult(proc.steps.head.id)(step3.id)
      assertResult(proc.steps(1).id)(step0.id)
      assertResult(proc.steps(2).id)(step1.id)
      assertResult(proc.steps.last.id)(step2.id)
    }
    "should move the first Step to the end" in {
      proc = proc.moveStep(0, 4)
      assert(proc.steps.length == 4)
      assertResult(proc.steps.head.id)(step0.id)
      assertResult(proc.steps(1).id)(step1.id)
      assertResult(proc.steps(2).id)(step2.id)
      assertResult(proc.steps.last.id)(step3.id)
    }
    "should remove a Step from the Process" in {
      val p = proc.removeStep(2) {
        case (id: ProcessId, id0: StepId) => List.empty[Task]
      }.get

      assert(p.steps.length == 3)
      assert(!p.steps.contains(step2))
      assertResult(p.steps.head.id)(step0.id)
      assertResult(p.steps.tail.head.id)(step1.id)
      assertResult(p.steps.last.id)(step3.id)
    }
  }

  "Tasks in a strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = Task.addToProcess(strictProcess, "card 1", None)
      assert(t.isDefined)
      assertResult(strictProcess._id)(t.get.processId)
      assertResult(stepId0)(t.get.stepId)
    }
    "move to next Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId1)(bid => Some(strictProcess))
      assert(t.isDefined)
      assertResult(strictProcess._id)(t.get.processId)
      assertResult(stepId1)(t.get.stepId)

      assert(t.isDefined)
      t = t.get.move(stepId2)(bid => Some(strictProcess))
      assert(t.isDefined)

      assert(t.isDefined)
      t = t.get.move(stepId3)(bid => Some(strictProcess))
      assert(t.isDefined)
    }
    "fail when moving back past previous Step" in {
      assert(t.get.move(stepId0)(bid => Some(strictProcess)).isEmpty)
    }
    "move to previous Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId2)(bid => Some(strictProcess))
      assert(t.isDefined)
      assertResult(strictProcess._id)(t.get.processId)
      assertResult(stepId2)(t.get.stepId)
    }
    "fail when moving beyond next Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId1)(bid => Some(strictProcess))

      assert(t.get.move(stepId3)(bid => Some(strictProcess)).isEmpty)
    }
  }

  "Tasks in a non-strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = Task.addToProcess(openProcess, "card 1", None)
      assert(t.isDefined)
      assertResult(openProcess._id)(t.get.processId)
      assertResult(stepId0)(t.get.stepId)
    }
    "move beyond next Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId3)(bid => Some(openProcess))
      assert(t.isDefined)
    }
    "move back past previous Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId0)(bid => Some(openProcess))
      assert(t.isDefined)
    }
    "move to next Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId1)(bid => Some(openProcess))
      assert(t.isDefined)
      assertResult(openProcess._id)(t.get.processId)
      assertResult(stepId1)(t.get.stepId)
    }
    "move to next Step again" in {
      assert(t.isDefined)
      t = t.get.move(stepId2)(bid => Some(openProcess))
      assert(t.isDefined)
      assert(t.isDefined)
      assertResult(openProcess._id)(t.get.processId)
      assertResult(stepId2)(t.get.stepId)
    }
    "move to previous Step" in {
      assert(t.isDefined)
      t = t.get.move(stepId3)(bid => Some(openProcess))
      assert(t.isDefined)
      assert(t.isDefined)
      assertResult(openProcess._id)(t.get.processId)
      assertResult(stepId3)(t.get.stepId)
    }
  }
}

object Processes {

  val pid1 = ProcessId(new ObjectId())
  val pid2 = ProcessId(new ObjectId())

  val openProcess = Process(
    _id = pid1,
    name = "Test Process",
    description = Some("Testing workflow in process"),
    steps = List(step0, step1, step2, step3)
  )

  val strictProcess = openProcess.copy(
    _id = pid2,
    strict = true
  )
}

object Steps {
  val stepId0 = StepId(new ObjectId())
  val stepId1 = StepId(new ObjectId())
  val stepId2 = StepId(new ObjectId())
  val stepId3 = StepId(new ObjectId())

  val step0 = SimpleStep(stepId0, "Backlog", Some("This is a backlog Step"))
  val step1 = SimpleStep(stepId1, "In Progress", Some("Work in progress"))
  val step2 = SimpleStep(stepId2, "Acceptance", Some("Trolling the internet"))
  val step3 = SimpleStep(stepId3, "Done", Some("All done amigo"))
}

