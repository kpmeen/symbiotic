/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe

import com.mongodb.casbah.TypeImports.ObjectId
import hipe.Hipe._
import hipe.steps.SimpleStep
import org.specs2._

class ProcessSpec extends mutable.Specification with TestProcesses {

  "A Process" should {
    var proc = Process(id = Some(ProcessId(new ObjectId())), name = "Test Process", description = Some("Just for testing"))

    "be initialized with no steps" in {
      proc.steps.isEmpty must_== true
    }

    "add a Step to the empty steps list" in {
      proc = appendStep(proc, step0)
      proc.steps.length must_== 1
    }
    "append a Step to the steps list" in {
      proc = appendStep(proc, step2)
      proc.steps.length must_== 2
    }
    "insert a Step after the first Step" in {
      proc = insertStep(proc, step1, 1)
      proc.steps.length must_== 3
      proc.steps.head.id must_== step0.id
      proc.steps.last.id must_== step2.id
      proc.steps(1).id must_== step1.id
    }
    "insert a Step before the second to last Step" in {
      proc = insertStep(proc, step3, 2)
      proc.steps.length must_== 4
      proc.steps.head.id must_== step0.id
      proc.steps(1).id must_== step1.id
      proc.steps(2).id must_== step3.id
      proc.steps.last.id must_== step2.id
    }
    "should move the second to last Step to the front" in {
      proc = moveStep(proc, 2, 0)
      proc.steps.length must_== 4
      proc.steps.head.id must_== step3.id
      proc.steps(1).id must_== step0.id
      proc.steps(2).id must_== step1.id
      proc.steps.last.id must_== step2.id
    }
    "should move the first Step to the end" in {
      proc = moveStep(proc, 0, 4)
      proc.steps.length must_== 4
      proc.steps.head.id must_== step0.id
      proc.steps(1).id must_== step1.id
      proc.steps(2).id must_== step2.id
      proc.steps.last.id must_== step3.id
    }
    "should remove a Step from the Process" in {
      val p = removeStep(proc, 2) {
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
      t = addToProcess(strictProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId0
    }
    "move to next Step" in {
      t.isDefined must_== true
      t = move(strictProcess, t.get, stepId1)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId1

      t.isDefined must_== true
      t = move(strictProcess, t.get, stepId2)
      t.isDefined must_== true
      t = move(strictProcess, t.get, stepId3)
      t.isDefined must_== true
    }
    "fail when moving back past previous Step" in {
      move(strictProcess, t.get, stepId0).isEmpty must_== true
    }
    "move to previous Step" in {
      t.isDefined must_== true
      t = move(strictProcess, t.get, stepId2)
      t.isDefined must_== true
      t.get.processId must_== strictProcess.id.get
      t.get.stepId must_== stepId2
    }
    "fail when moving beyond next Step" in {
      t.isDefined must_== true
      t = move(strictProcess, t.get, stepId1)
      move(strictProcess, t.get, stepId3).isEmpty must_== true
    }
  }

  "Tasks in a non-strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = addToProcess(openProcess, "card 1", None)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId0
    }
    "move beyond next Step" in {
      t.isDefined must_== true
      t = move(openProcess, t.get, stepId3)
      t.isDefined must_== true
    }
    "move back past previous Step" in {
      t.isDefined must_== true
      t = move(openProcess, t.get, stepId0)
      t.isDefined must_== true
    }
    "move to next Step" in {
      t.isDefined must_== true
      t = move(openProcess, t.get, stepId1)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId1
    }
    "move to next Step again" in {
      t.isDefined must_== true
      t = move(openProcess, t.get, stepId2)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId2
    }
    "move to previous Step" in {
      t.isDefined must_== true
      t = move(openProcess, t.get, stepId3)
      t.isDefined must_== true
      t.get.processId must_== openProcess.id.get
      t.get.stepId must_== stepId3
    }
  }
}

trait TestProcesses extends TestSteps {

  val pid1 = ProcessId(new ObjectId())
  val pid2 = ProcessId(new ObjectId())

  val openProcess = Process(
    id = Some(pid1),
    name = "Test Process",
    description = Some("Testing workflow in process"),
    steps = List(step0, step1, step2, step3)
  )

  val strictProcess = openProcess.copy(
    id = Some(pid2),
    strict = true
  )
}

trait TestSteps {
  val stepId0 = StepId(new ObjectId())
  val stepId1 = StepId(new ObjectId())
  val stepId2 = StepId(new ObjectId())
  val stepId3 = StepId(new ObjectId())

  val step0 = SimpleStep(stepId0, "Backlog", Some("This is a backlog Step"))
  val step1 = SimpleStep(stepId1, "In Progress", Some("Work in progress"))
  val step2 = SimpleStep(stepId2, "Acceptance", Some("Trolling the internet"))
  val step3 = SimpleStep(stepId3, "Done", Some("All done amigo"))
}

