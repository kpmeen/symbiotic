/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package workflow.engine

import com.mongodb.casbah.TypeImports.ObjectId
import models.core.{Id, WithIdTransformers}
import play.api.libs.json._

sealed trait WorkflowId extends Id

case class ProcessId(id: ObjectId) extends WorkflowId

case class StepId(id: ObjectId = new ObjectId) extends WorkflowId

case class TaskId(id: ObjectId = new ObjectId) extends WorkflowId

object ProcessId extends WithIdTransformers {
  implicit val boardIdReads: Reads[ProcessId] = reads[ProcessId](ProcessId.apply)
  implicit val boardIdWrites: Writes[ProcessId] = writes[ProcessId]
}

object StepId extends WithIdTransformers {
  implicit val columnIdReads: Reads[StepId] = reads[StepId](StepId.apply)
  implicit val columnIdWrites: Writes[StepId] = writes[StepId]
}

object TaskId extends WithIdTransformers {
  implicit val cardIdReads: Reads[TaskId] = reads[TaskId](TaskId.apply)
  implicit val cardIdWrites: Writes[TaskId] = writes[TaskId]
}