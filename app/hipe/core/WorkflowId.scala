/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import core.converters.WithIdConverters
import models.base.Id
import play.api.libs.json._

sealed trait WorkflowId extends Id

case class ProcessId(id: String) extends WorkflowId

case class StepId(id: String) extends WorkflowId

case class TaskId(id: String) extends WorkflowId

object ProcessId extends WithIdConverters[ProcessId] {
  implicit val processIdReads: Reads[ProcessId] = reads(ProcessId.apply)
  implicit val processIdWrites: Writes[ProcessId] = writes

  override implicit def asId(s: String): ProcessId = ProcessId(s)
}

object StepId extends WithIdConverters[StepId] {
  implicit val stepIdReads: Reads[StepId] = reads(StepId.apply)
  implicit val stepIdWrites: Writes[StepId] = writes

  override implicit def asId(s: String): StepId = StepId(s)
}

object TaskId extends WithIdConverters[TaskId] {
  implicit val taskIdReads: Reads[TaskId] = reads(TaskId.apply)
  implicit val taskIdWrites: Writes[TaskId] = writes

  override implicit def asId(s: String): TaskId = TaskId(s)
}