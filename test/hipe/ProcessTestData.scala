/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.{Process, ProcessId, Step, StepId}

trait ProcessTestData {

  val pid1 = ProcessId.create()
  val pid2 = ProcessId.create()

  val stepId0 = StepId.create()
  val stepId1 = StepId.create()
  val stepId2 = StepId.create()
  val stepId3 = StepId.create()

  val step0 = Step(Some(stepId0), "Backlog", Some("This is a backlog Step"))
  val step1 = Step(Some(stepId1), "In Progress", Some("Work in progress"))
  val step2 = Step(Some(stepId2), "Acceptance", Some("Trolling the internet"))
  val step3 = Step(Some(stepId3), "Done", Some("All done amigo"))

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
