/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core._
import models.parties.UserId

trait ProcessTestData {

  val uid0 = UserId.create()
  val uid1 = UserId.create()
  val uid2 = UserId.create()

  val pid1 = ProcessId.create()
  val pid2 = ProcessId.create()

  val stepId0 = StepId.create()
  val stepId1 = StepId.create()
  val stepId2 = StepId.create()
  val stepId3 = StepId.create()

  val sgId0 = StepGroupId.create()
  val sgId1 = StepGroupId.create()
  val sgId2 = StepGroupId.create()
  val sgId3 = StepGroupId.create()

  val step0 = Step(id = Some(stepId0), name = "Backlog", description = Some("This is a backlog Step"))
  val step1 = Step(id = Some(stepId1), name = "In Progress", description = Some("Work in progress"))
  val step2 = Step(id = Some(stepId2), name = "Acceptance", description = Some("Trolling the internet"))
  val step3 = Step(id = Some(stepId3), name = "Done", description = Some("All done amigo"))

  val strictStep0 = step0.copy(minAssignments = 1, minCompleted = 0)
  val strictStep1 = step1.copy(minAssignments = 2, minCompleted = 1)
  val strictStep2 = step2.copy(minAssignments = 2, minCompleted = 2)
  val strictStep3 = step3.copy(minAssignments = 1, minCompleted = 1)

  val sg0 = StepGroup(id = Some(sgId0), name = Some("sg0"), steps = StepList(step0))
  val sg1 = StepGroup(id = Some(sgId1), name = Some("sg1"), steps = StepList(step1))
  val sg2 = StepGroup(id = Some(sgId2), name = Some("sg2"), steps = StepList(step2))
  val sg3 = StepGroup(id = Some(sgId3), name = Some("sg3"), steps = StepList(step3))

  val openStepGroupList = StepGroupList(sg0, sg1, sg2, sg3)

  val openProcess = Process(
    id = Some(pid1),
    name = "Test Process",
    description = Some("Testing workflow in process"),
    stepGroups = openStepGroupList
  )

  val strictGroup0 = StepGroup(
    id = Some(sgId0),
    name = Some("sg0"),
    steps = StepList(strictStep0)
  )
  val strictGroup1 = StepGroup(
    id = Some(sgId1),
    name = Some("sg1"),
    priv = true,
    steps = StepList(strictStep1, strictStep2)
  )
  val strictGroup2 = StepGroup(
    id = Some(sgId2),
    name = Some("sg2"),
    steps = StepList(strictStep3)
  )

  val strictStepGroupList = StepGroupList(strictGroup0, strictGroup1, strictGroup2)

  val strictProcess = openProcess.copy(
    id = Some(pid2),
    strict = true,
    stepGroups = strictStepGroupList
  )

}
