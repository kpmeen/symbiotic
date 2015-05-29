/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations._
import hipe.core._
import org.specs2._

class ProcessOperationsSpec extends mutable.Specification with ProcessOperations with ProcessTestData {

  "When configuring a simple process with only one Step per StepGroup it" should {
    var proc = Process(id = Some(ProcessId.create()), name = "Test Process", description = Some("Just for testing"))

    "be initialized with no steps" in {
      proc.stepGroups.isEmpty must_== true
    }

    "be possible to add a Step to the empty steps list" in {
      proc = appendStep(proc, step0)
      proc.stepGroups.flatten.length must_== 1
    }
    "be possible to append a Step to the steps list" in {
      proc = appendStep(proc, step2)
      proc.stepGroups.flatten.length must_== 2
    }
    "be possible to insert a Step after the first Step" in {
      proc = insertStep(proc, step1, 1)
      val steps = proc.stepGroups.flatten
      steps.length must_== 3
      steps.head.id must_== step0.id
      steps.last.id must_== step2.id
      steps(1).id must_== step1.id
    }
    "be possible to insert a Step before the second to last Step" in {
      proc = insertStep(proc, step3, 2)
      val steps = proc.stepGroups.flatten
      steps.length must_== 4
      steps.head.id must_== step0.id
      steps(1).id must_== step1.id
      steps(2).id must_== step3.id
      steps.last.id must_== step2.id
    }
    "be possible to move the second to last Step to the front" in {
      proc = moveStepGroup(proc, 2, 0)
      val steps = proc.stepGroups.flatten
      steps.length must_== 4
      steps.head.id must_== step3.id
      steps(1).id must_== step0.id
      steps(2).id must_== step1.id
      steps.last.id must_== step2.id
    }
    "be possible to move the first Step to the end" in {
      proc = moveStepGroup(proc, 0, 4)
      val steps = proc.stepGroups.flatten
      steps.length must_== 4
      steps.head.id must_== step0.id
      steps(1).id must_== step1.id
      steps(2).id must_== step2.id
      steps.last.id must_== step3.id
    }
    "be possible to remove a Step from the Process" in {
      val p1 = removeStep(proc, stepId2) {
        case (pid: ProcessId, sid: StepId) => List.empty[Task]
      }
      p1.isRight must_== true
      val p = p1.right.get
      val steps = p.stepGroups.flatten
      steps.length must_== 3
      steps.contains(step2) must_== false
      steps.head.id must_== step0.id
      steps.tail.head.id must_== step1.id
      steps.last.id must_== step3.id
    }
  }

  "When configuring a process with more than one Step in a StepGroup it" should {
    var proc = Process(id = ProcessId.createOpt(), name = "Test Process", description = Some("Just for testing"), strict = true)

    "be initialized with no steps" in {
      proc.stepGroups.isEmpty must_== true
    }

    "be possible to append a Step to the empty steps list" in {
      proc = appendStep(proc, strictStep0)
      proc.stepGroups.flatten.length must_== 1
    }
    "be possible to append a Step to the steps list" in {
      proc = appendStep(proc, strictStep2)
      proc.stepGroups.flatten.length must_== 2
    }

    // TODO: Define test cases for how the interaction with step groups should be.

    "tbd..." in {
      pending("TBD...")
    }
  }

}

