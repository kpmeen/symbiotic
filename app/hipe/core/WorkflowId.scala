/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import core.converters.{WithIdConverters, WithDBIdConverters}
import models.base.{Id, DBId}
import play.api.libs.json._

case class ProcessId(value: String) extends DBId

case class StepId(value: String) extends Id

case class TaskId(value: String) extends DBId

case class AssignmentId(value: String) extends Id

object ProcessId extends WithDBIdConverters[ProcessId] {

  implicit val processIdReads: Reads[ProcessId] = reads(ProcessId.apply)
  implicit val processIdWrites: Writes[ProcessId] = writes

  override implicit def asId(s: String): ProcessId = ProcessId(s)
}

object StepId extends WithIdConverters[StepId] {

  implicit val stepIdReads: Reads[StepId] = reads(StepId.apply)
  implicit val stepIdWrites: Writes[StepId] = writes

  override implicit def asId(s: String): StepId = StepId(s)
}

object TaskId extends WithDBIdConverters[TaskId] {

  implicit val taskIdReads: Reads[TaskId] = reads(TaskId.apply)
  implicit val taskIdWrites: Writes[TaskId] = writes

  override implicit def asId(s: String): TaskId = TaskId(s)
}

object AssignmentId extends WithIdConverters[AssignmentId] {

  implicit val assignmentIdReads: Reads[AssignmentId] = reads(AssignmentId.apply)
  implicit val assignmentIdWrites: Writes[AssignmentId] = writes

  override implicit def asId(s: String): AssignmentId = AssignmentId(s)
}