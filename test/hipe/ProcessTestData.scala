/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.{Process, ProcessId, Step, StepId}
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

  val step0 = Step( id = Some(stepId0), name = "Backlog", description = Some("This is a backlog Step"))
  val step1 = Step(id = Some(stepId1), name = "In Progress", description = Some("Work in progress"))
  val step2 = Step(id = Some(stepId2), name = "Acceptance", description = Some("Trolling the internet"))
  val step3 = Step( id = Some(stepId3), name = "Done", description = Some("All done amigo"))

  val openStepList = List(step0, step1, step2, step3)

  val strictStepList = List(
    step0.copy(minAssignments = 1, minCompleted = 0),
    step1.copy(minAssignments = 2, minCompleted = 1),
    step2.copy(minAssignments = 2, minCompleted = 2),
    step3.copy(minAssignments = 1, minCompleted = 1)
  )

  val openProcess = Process(
    id = Some(pid1),
    name = "Test Process",
    description = Some("Testing workflow in process"),
    stepList = openStepList
  )

  val strictProcess = openProcess.copy(
    id = Some(pid2),
    strict = true,
    stepList = strictStepList
  )

}
