/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import core.converters.IdConverters
import models.base.Id
import play.api.libs.json._

case class ProcessId(value: String) extends Id

case class StepGroupId(value: String) extends Id

case class StepId(value: String) extends Id

case class TaskId(value: String) extends Id

case class AssignmentId(value: String) extends Id

object ProcessId extends IdConverters[ProcessId] {
  implicit val r: Reads[ProcessId] = reads(ProcessId.apply)
  implicit val w: Writes[ProcessId] = writes

  override implicit def asId(s: String): ProcessId = ProcessId(s)
}

object StepGroupId extends IdConverters[StepGroupId] {
  implicit val r: Reads[StepGroupId] = reads(StepGroupId.apply)
  implicit val w: Writes[StepGroupId] = writes

  override implicit def asId(s: String): StepGroupId = StepGroupId(s)
}

object StepId extends IdConverters[StepId] {
  implicit val r: Reads[StepId] = reads(StepId.apply)
  implicit val w: Writes[StepId] = writes

  override implicit def asId(s: String): StepId = StepId(s)
}

object TaskId extends IdConverters[TaskId] {
  implicit val r: Reads[TaskId] = reads(TaskId.apply)
  implicit val w: Writes[TaskId] = writes

  override implicit def asId(s: String): TaskId = TaskId(s)
}

object AssignmentId extends IdConverters[AssignmentId] {
  implicit val r: Reads[AssignmentId] = reads(AssignmentId.apply)
  implicit val w: Writes[AssignmentId] = writes

  override implicit def asId(s: String): AssignmentId = AssignmentId(s)
}