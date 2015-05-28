/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.{SymbioticDB, WithMongoIndex}
import hipe.core.AssignmentDetails.Assignment
import hipe.core.States.TaskState
import models.base.{PersistentType, PersistentTypeConverters}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

/**
 * The interesting bit...a Task is what is moved around through the Steps during the Process life-cycle.
 *
 * TODO:
 * Add type argument and create a trait for data classes to be "processable" to implement.
 * This trait needs to have a bare-bones set of functions to call/impl by the data class.
 *
 * maybe like this...
 * {{{
 *  case class Task[ID <: Id, T <: Processable](..., dataRefId: Option[ID], data: Option[T])
 * }}}
 */
case class Task(
  _id: Option[ObjectId] = None,
  id: Option[TaskId] = None,
  processId: ProcessId,
  stepId: StepId,
  title: String,
  description: Option[String] = None,
  state: TaskState,
  assignments: Seq[Assignment] = Seq.empty) extends PersistentType {

  def updateAssignment(func: (Seq[Assignment]) => Option[Assignment]): Seq[Assignment] = {
    val maybeAssigns = func(assignments)
    maybeAssigns.map { a =>
      val origIndex = assignments.indexWhere(_.id == a.id)
      assignments.updated(origIndex, a)
    }.getOrElse(assignments)
  }

}

object Task extends PersistentTypeConverters with ObjectBSONConverters[Task] with DateTimeConverters with SymbioticDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(classOf[Task])

  implicit val taskReads = Json.reads[Task]
  implicit val taskWrites = Json.writes[Task]

  implicit override def toBSON(t: Task): DBObject = {
    val builder = MongoDBObject.newBuilder
    t._id.foreach(builder += "_id" -> _)
    t.id.foreach(builder += "id" -> _.value)
    builder += "processId" -> t.processId.value
    builder += "stepId" -> t.stepId.value
    builder += "title" -> t.title
    t.description.foreach(builder += "description" -> _)
    builder += "state" -> TaskState.asString(t.state)
    builder += "assignments" -> t.assignments.map(Assignment.toBSON)

    builder.result()
  }

  override def fromBSON(dbo: DBObject): Task =
    Task(
      _id = dbo.getAs[ObjectId]("_id"),
      id = dbo.getAs[String]("id"),
      processId = dbo.as[String]("processId"),
      stepId = dbo.as[String]("stepId"),
      title = dbo.as[String]("title"),
      description = dbo.getAs[String]("description"),
      state = dbo.as[String]("state"),
      assignments = dbo.getAs[Seq[DBObject]]("assignments").map(_.map(Assignment.fromBSON)).getOrElse(Seq.empty)
    )

  override val collectionName: String = "tasks"

  override def ensureIndex(): Unit = ???

  def findById(taskId: TaskId): Option[Task] =
    collection.findOne(MongoDBObject("id" -> taskId.value)).map(tct => fromBSON(tct))

  def findByProcessId(procId: ProcessId): List[Task] = {
    collection.find(MongoDBObject("processId" -> procId.value)).map(Task.fromBSON).toList
  }

  def save(task: Task) = {
    val res = collection.save(task)

    if (res.isUpdateOfExisting) logger.info(s"Updated existing Task with Id ${task.id}")
    else println(s"Inserted new Task with Id ${Option(res.getUpsertedId).getOrElse(task.id)}")
  }

}
