/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations._
import hipe.core._
import org.specs2._

class ProcessOperationsSpec extends mutable.Specification with ProcessOperations with ProcessTestData {

  "When configuring a process it" should {
    var proc = Process(id = Some(ProcessId.create()), name = "Test Process", description = Some("Just for testing"))

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

}

