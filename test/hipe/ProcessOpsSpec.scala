/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe

import com.mongodb.casbah.TypeImports.ObjectId
import hipe.core._
import org.specs2._

class ProcessOpsSpec extends mutable.Specification with TestProcesses {

  "When configuring a process it" should {
    var proc = Process(id = Some(ProcessId(new ObjectId().toString)), name = "Test Process", description = Some("Just for testing"))

    "be initialized with no steps" in {
      proc.stepList.isEmpty must_== true
    }

    "be possible to add a Step to the empty steps list" in {
      proc = appendStep(proc, step0)
      proc.stepList.length must_== 1
    }
    "be possible to append a Step to the steps list" in {
      proc = appendStep(proc, step2)
      proc.stepList.length must_== 2
    }
    "be possible to insert a Step after the first Step" in {
      proc = insertStep(proc, step1, 1)
      proc.stepList.length must_== 3
      proc.stepList.head.id must_== step0.id
      proc.stepList.last.id must_== step2.id
      proc.stepList(1).id must_== step1.id
    }
    "be possible to insert a Step before the second to last Step" in {
      proc = insertStep(proc, step3, 2)
      proc.stepList.length must_== 4
      proc.stepList.head.id must_== step0.id
      proc.stepList(1).id must_== step1.id
      proc.stepList(2).id must_== step3.id
      proc.stepList.last.id must_== step2.id
    }
    "be possible to move the second to last Step to the front" in {
      proc = moveStep(proc, 2, 0)
      proc.stepList.length must_== 4
      proc.stepList.head.id must_== step3.id
      proc.stepList(1).id must_== step0.id
      proc.stepList(2).id must_== step1.id
      proc.stepList.last.id must_== step2.id
    }
    "be possible to move the first Step to the end" in {
      proc = moveStep(proc, 0, 4)
      proc.stepList.length must_== 4
      proc.stepList.head.id must_== step0.id
      proc.stepList(1).id must_== step1.id
      proc.stepList(2).id must_== step2.id
      proc.stepList.last.id must_== step3.id
    }
    "be possible to remove a Step from the Process" in {
      val p = removeStep(proc, 2) {
        case (id: ProcessId, id0: StepId) => List.empty[Task]
      }.get

      p.stepList.length must_== 3
      p.stepList.contains(step2) must_== false
      p.stepList.head.id must_== step0.id
      p.stepList.tail.head.id must_== step1.id
      p.stepList.last.id must_== step3.id
    }
  }

  "Tasks in a strict Process" should {
    var t: Option[Task] = None

    "be added in the first Step" in {
      t = addTaskToProcess(strictProcess, "card 1", None)
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
      t = addTaskToProcess(openProcess, "card 1", None)
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

trait TestProcesses extends TestSteps with ProcessOperations with TaskOperations {

  val pid1 = ProcessId(new ObjectId().toString)
  val pid2 = ProcessId(new ObjectId().toString)

  val openProcess = Process(
    id = Some(pid1),
    name = "Test Process",
    description = Some("Testing workflow in process"),
    stepList = List(step0, step1, step2, step3)
  )

  val strictProcess = openProcess.copy(
    id = Some(pid2),
    strict = true
  )
}

trait TestSteps {
  val stepId0 = StepId(new ObjectId().toString)
  val stepId1 = StepId(new ObjectId().toString)
  val stepId2 = StepId(new ObjectId().toString)
  val stepId3 = StepId(new ObjectId().toString)

  val step0 = Step(stepId0, "Backlog", Some("This is a backlog Step"))
  val step1 = Step(stepId1, "In Progress", Some("Work in progress"))
  val step2 = Step(stepId2, "Acceptance", Some("Trolling the internet"))
  val step3 = Step(stepId3, "Done", Some("All done amigo"))
}

