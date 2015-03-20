/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package core.workflow.engine

import com.mongodb.casbah.TypeImports.ObjectId
import core.converters.WithIdConverters
import models.base.Id
import play.api.libs.json._

sealed trait WorkflowId extends Id

case class ProcessId(id: ObjectId) extends WorkflowId

case class StepId(id: ObjectId = new ObjectId) extends WorkflowId

case class TaskId(id: ObjectId = new ObjectId) extends WorkflowId

object ProcessId extends WithIdConverters[ProcessId] {
  implicit val boardIdReads: Reads[ProcessId] = reads(ProcessId.apply)
  implicit val boardIdWrites: Writes[ProcessId] = writes

  override implicit def asId(oid: ObjectId): ProcessId = ProcessId(oid)

  override implicit def asId(s: String): ProcessId = ProcessId(new ObjectId(s))
}

object StepId extends WithIdConverters[StepId] {
  implicit val columnIdReads: Reads[StepId] = reads(StepId.apply)
  implicit val columnIdWrites: Writes[StepId] = writes

  override implicit def asId(oid: ObjectId): StepId = StepId(oid)

  override implicit def asId(s: String): StepId = StepId(new ObjectId(s))
}

object TaskId extends WithIdConverters[TaskId] {
  implicit val cardIdReads: Reads[TaskId] = reads(TaskId.apply)
  implicit val cardIdWrites: Writes[TaskId] = writes

  override implicit def asId(oid: ObjectId): TaskId = TaskId(oid)

  override implicit def asId(s: String): TaskId = TaskId(new ObjectId(s))
}