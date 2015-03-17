/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package core.workflow.engine

import com.mongodb.casbah.TypeImports.ObjectId
import core.workflow.engine.Processes._
import core.workflow.engine.Steps._
import org.specs2._

class ProcessSpec extends mutable.Specification {

  "A Process" should {
    var proc = Process(id = ProcessId(new ObjectId()), name = "Test Process", description = Some("Just for testing"))

    "be initialized with no steps" in {
      proc.steps.isEmpty must_== true
    }

    "add a Step to the empty steps list" in {
      proc = proc.appendStep(step0)
      proc.steps.length must_== 1
    }
    "append a Step to the steps list" in {
      proc = proc.appendStep(step2)
      proc.steps.length must_== 2
    }
    "insert a Step after the first Step" in {
      proc = proc.insertStep(step1, 1)
      proc.steps.length must_== 3
      proc.steps.head.id must_== step0.id
      proc.steps.last.id must_== step2.id
      proc.steps(1).id must_== step1.id
    }
    "insert a Step before the second to last Step" in {
      proc = proc.insertStep(step3, 2)
      proc.steps.length must_== 4
      proc.steps.head.id must_== step0.id
      proc.steps(1).id must_== step1.id
      proc.steps(2).id must_== step3.id
      proc.steps.last.id must_== step2.id
    }
    "should move the second to last Step to the front" in {
      proc = proc.moveStep(2, 0)
      proc.steps.length must_== 4
      proc.steps.head.id must_== step3.id
      proc.steps(1).id must_== step0.id
      proc.steps(2).id must_== step1.id
      proc.steps.last.id must_== step2.id
    }
    "should move the first Step to the end" in {
      proc = proc.moveStep(0, 4)
      proc.steps.length must_== 4
      proc.steps.head.id must_== step0.id
      proc.steps(1).id must_== step1.id
      proc.steps(2).id must_== step2.id
      proc.steps.last.id must_== step3.id
    }
    "should remove a Step from the Process" in {
      val p = proc.removeStep(2) {
        case (id: ProcessId, id0: StepId) => List.empty[Task]
      }.get

      p.steps.length must_== 3
      p.steps.contains(step2) must_== false
      p.steps.head.id must_== step0.id
      p.steps.tail.head.id must_== step1.id
      p.steps.last.id must_== step3.id
    }
  }

  "Tasks in a strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = Task.addToProcess(strictProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id
      t.get.stepId must_== stepId0
    }
    "move to next Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId1)(bid => Some(strictProcess))
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id
      t.get.stepId must_== stepId1

      t.isDefined must_== true
      t = t.get.move(stepId2)(bid => Some(strictProcess))
      t.isDefined must_== true
      t = t.get.move(stepId3)(bid => Some(strictProcess))
      t.isDefined must_== true
    }
    "fail when moving back past previous Step" in {
      t.get.move(stepId0)(bid => Some(strictProcess)).isEmpty must_== true
    }
    "move to previous Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId2)(bid => Some(strictProcess))
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id
      t.get.stepId must_== stepId2
    }
    "fail when moving beyond next Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId1)(bid => Some(strictProcess))
      t.get.move(stepId3)(bid => Some(strictProcess)).isEmpty must_== true
    }
  }

  "Tasks in a non-strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = Task.addToProcess(openProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id
      t.get.stepId must_== stepId0
    }
    "move beyond next Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId3)(bid => Some(openProcess))
      t.isDefined must_== true
    }
    "move back past previous Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId0)(bid => Some(openProcess))
      t.isDefined must_== true
    }
    "move to next Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId1)(bid => Some(openProcess))
      t.isDefined must_== true
      t.get.processId must_== openProcess.id
      t.get.stepId must_== stepId1
    }
    "move to next Step again" in {
      t.isDefined must_== true
      t = t.get.move(stepId2)(bid => Some(openProcess))
      t.isDefined must_== true
      t.get.processId must_== openProcess.id
      t.get.stepId must_== stepId2
    }
    "move to previous Step" in {
      t.isDefined must_== true
      t = t.get.move(stepId3)(bid => Some(openProcess))
      t.isDefined must_== true
      t.get.processId must_== openProcess.id
      t.get.stepId must_== stepId3
    }
  }
}

object Processes {

  val pid1 = ProcessId(new ObjectId())
  val pid2 = ProcessId(new ObjectId())

  val openProcess = Process(
    id = pid1,
    name = "Test Process",
    description = Some("Testing workflow in process"),
    steps = List(step0, step1, step2, step3)
  )

  val strictProcess = openProcess.copy(
    id = pid2,
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

