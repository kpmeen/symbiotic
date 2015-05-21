/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.{WithDateTimeConverters, WithObjectBSONConverters}
import core.mongodb.{WithMongo, WithMongoIndex}
import hipe.core.AssignmentDetails.Assignment
import play.api.Logger
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
  id: Option[TaskId] = None,
  processId: ProcessId,
  stepId: StepId,
  title: String,
  description: Option[String] = None,
  assignments: Seq[Assignment] = Seq.empty) {

  def updateAssignment(func: (Seq[Assignment]) => Option[Assignment]): Seq[Assignment] = {
    val maybeAssigns = func(assignments)
    maybeAssigns.map { a =>
      val origIndex = assignments.indexWhere(_.id == a.id)
      assignments.updated(origIndex, a)
    }.getOrElse(assignments)
  }

}

object Task extends WithObjectBSONConverters[Task] with WithDateTimeConverters with WithMongo with WithMongoIndex {

  val logger = Logger(classOf[Task])

  implicit val taskReads = Json.reads[Task]
  implicit val taskWrites = Json.writes[Task]

  implicit override def toBSON(t: Task): DBObject = {
    val builder = MongoDBObject.newBuilder
    t.id.foreach(builder += "_id" -> _.asOID)
    builder += "processId" -> t.processId.asOID
    builder += "stepId" -> t.stepId.value
    builder += "title" -> t.title
    t.description.foreach(builder += "description" -> _)
    builder += "assignments" -> t.assignments.map(Assignment.toBSON)

    builder.result()
  }

  override def fromBSON(dbo: DBObject): Task =
    Task(
      id = TaskId.asOptId(dbo.getAs[ObjectId]("_id")),
      processId = ProcessId.asId(dbo.getAs[ObjectId]("processId").get),
      stepId = StepId.asId(dbo.getAs[String]("stepId").get),
      title = dbo.getAs[String]("title").get,
      description = dbo.getAs[String]("description"),
      assignments = dbo.getAs[Seq[DBObject]]("assignments").map(_.map(Assignment.fromBSON)).getOrElse(Seq.empty)
    )

  override val collectionName: String = "tasks"

  override def ensureIndex(): Unit = ???

  def findById(taskId: TaskId): Option[Task] = collection.findOneByID(taskId.asOID).map(tct => fromBSON(tct))

  def findByProcessId(procId: ProcessId): List[Task] = {
    collection.find(MongoDBObject("processId" -> procId.asOID)).map(Task.fromBSON).toList
  }

  def save(task: Task) = {
    val res = collection.save(task)

    if (res.isUpdateOfExisting) logger.info(s"Updated existing Task with Id ${task.id}")
    else println(s"Inserted new Task with Id ${Option(res.getUpsertedId).getOrElse(task.id)}")
  }

}
