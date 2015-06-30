/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core._
import org.specs2._

class ProcessOperationsSpec extends mutable.Specification with ProcessOperations with ProcessTestData {

  sequential

  "When configuring a simple process with only one Step per StepGroup it" should {
    var proc = Process(id = ProcessId.createOpt(), name = "Test Process", description = Some("Just for testing"))

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
      proc = insertStep(proc, step4, 2)
      val steps = proc.stepGroups.flatten
      steps.length must_== 4
      steps.head.id must_== step0.id
      steps(1).id must_== step1.id
      steps(2).id must_== step4.id
      steps.last.id must_== step2.id
    }
    "be possible to move the second to last Step to the front" in {
      val res = moveStepGroup(proc, proc.stepGroups(2).id.get, 0)
      res.isRight must_== true
      proc = res.right.get
      val steps = proc.stepGroups.flatten
      steps.length must_== 4
      steps.head.id must_== step4.id
      steps(1).id must_== step0.id
      steps(2).id must_== step1.id
      steps.last.id must_== step2.id
    }
    "be possible to move the first Step to the end" in {
      val res = moveStepGroup(proc, proc.stepGroups.head.id.get, 4)
      res.isRight must_== true
      proc = res.right.get
      val steps = proc.stepGroups.flatten
      steps.length must_== 4
      steps.head.id must_== step0.id
      steps(1).id must_== step1.id
      steps(2).id must_== step2.id
      steps.last.id must_== step4.id
    }
    "be possible to remove a Step" in {
      val p1 = removeStep(proc, stepId2)
      p1.isRight must_== true
      val p = p1.right.get
      val steps = p.stepGroups.flatten
      steps.length must_== 3
      steps.contains(step2) must_== false
      steps.head.id must_== step0.id
      steps.tail.head.id must_== step1.id
      steps.last.id must_== step4.id
    }
  }

  "When configuring a process with more than one Step in a StepGroup it" should {
    var p = Process(id = ProcessId.createOpt(), name = "Test2", description = Some("Foo bar"), strict = true)

    "be initialized with no steps" in {
      p.stepGroups.isEmpty must_== true
    }

    "be possible to append a Step to the empty steps list" in {
      p = appendStep(p, strictStep0)
      p.stepGroups.size must_== 1
      p.stepGroups.flatten.size must_== 1
    }
    "be possible to append another a Step to the steps list" in {
      p = appendStep(p, strictStep1)
      p.stepGroups.flatten.length must_== 2
    }
    "be possible to append a Step to the same StepGroup as the second Step" in {
      val orig = p
      val res = appendStepToGroup(p, p.stepGroups.tail.head.id.get, strictStep4)
      res.isRight must_== true
      p = res.right.get
      val grp = p.stepGroups.find(_.id.contains(p.stepGroups.last.id.get))
      grp must_!= None
      grp.get.steps.size must_== 2
      grp.get.steps(0) must_== strictStep1
      grp.get.steps(1) must_== strictStep4
      p.stepGroups.flatten.size must_== orig.stepGroups.flatten.size + 1
    }
    "be possible to insert a Step between the first and second steps of the second StepGroup" in {
      val orig = p
      val sgid = p.stepGroups.last.id.get
      val res = insertStepToGroup(p, sgid, strictStep2, 1)
      res.isRight must_== true
      p = res.right.get
      val grp = p.stepGroups.find(_.id.contains(sgid))
      grp must_!= None
      grp.get.steps.size must_== 3
      grp.get.steps(0) must_== strictStep1
      grp.get.steps(1) must_== strictStep2
      grp.get.steps(2) must_== strictStep4
      p.stepGroups.flatten.size must_== orig.stepGroups.flatten.size + 1
    }
    "be possible to move a Step within the bounds of a StepGroup" in {
      val sgid = p.stepGroups.last.id.get
      val res = moveStepInGroup(p, sgid, strictStep4.id.get, 1)
      res.isRight must_== true
      val r = res.right.get
      val grp = r.stepGroups.find(_.id.contains(sgid))
      grp must_!= None
      grp.get.steps.size must_== 3
      grp.get.steps(0) must_== strictStep1
      grp.get.steps(1) must_== strictStep4
      grp.get.steps(2) must_== strictStep2
      r.stepGroups.flatten.size must_== p.stepGroups.flatten.size
    }
    "be possible to move a Step out of a StepGroup and into another" in {
      val from = p.stepGroups.last.id.get
      val to = p.stepGroups.head.id.get

      val res = moveStepToGroup(p, strictStep4.id.get, to, 0)
      res.isRight must_== true
      val r = res.right.get

      val og = r.stepGroups.find(_.id.contains(from))
      og must_!= None
      og.get.steps.size must_== 2
      og.get.steps(0) must_== strictStep1
      og.get.steps(1) must_== strictStep2

      val ng = r.stepGroups.find(_.id.contains(to))
      ng must_!= None
      ng.get.steps.size must_== 2
      ng.get.steps(0) must_== strictStep4
      ng.get.steps(1) must_== strictStep0

      r.stepGroups.flatten.size must_== p.stepGroups.flatten.size
    }
    "be possible to move a Step out of a StepGroup into a new StepGroup" in {
      val orig = p

      val res = moveStepToNewGroup(p, strictStep4.id.get, 2)
      res.isRight must_== true
      p = res.right.get

      val og = p.stepGroups.last
      og.steps.size must_== 2
      og.steps(0) must_== strictStep1
      og.steps(1) must_== strictStep2

      val ng = p.stepGroups.tail.head
      ng.steps.size must_== 1
      ng.steps(0) must_== strictStep4

      p.stepGroups.size must_== orig.stepGroups.size + 1
      p.stepGroups.flatten.size must_== orig.stepGroups.flatten.size
    }
    "be possible to move a StepGroup with all its steps" in {
      val orig = p
      val res = moveStepGroup(p, p.stepGroups(2).id.get, 1)
      res.isRight must_== true
      p = res.right.get

      val g1 = p.stepGroups(0)
      val g2 = p.stepGroups(1)
      val g3 = p.stepGroups(2)

      g1.steps.size must_== 1
      g1.steps(0) must_== strictStep0

      g2.steps.size must_== 2
      g2.steps(0) must_== strictStep1
      g2.steps(1) must_== strictStep2

      g3.steps.size must_== 1
      g3.steps(0) must_== strictStep4

      p.stepGroups.size must_== orig.stepGroups.size
      p.stepGroups.flatten.size must_== orig.stepGroups.flatten.size
    }
    "be possible to remove a Step within a StepGroup" in {
      val res = removeStep(p, strictStep1.id.get)
      res.isRight must_== true
      val r = res.right.get
      val steps = r.stepGroups.flatten
      steps.size must_== p.stepGroups.flatten.size - 1
      steps.exists(_.id == strictStep1.id) must_== false
    }
    "be possible to remove an entire StepGroup and all its content" in {
      val remId = p.stepGroups.last.id.get
      val res = removeGroup(p, remId)
      res.isRight must_== true
      val r = res.right.get
      r.stepGroups.size must_== p.stepGroups.size - 1
      r.stepGroups.exists(_.id.contains(remId)) must_== false
    }

  }
}

