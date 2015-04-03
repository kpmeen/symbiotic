/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import _root_.core.converters.{WithBSONConverters, WithDateTimeConverters}
import _root_.core.mongodb.WithMongo
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import models.parties.UserId
import play.api.Logger
import play.api.libs.json.Json

/**
 * The interesting bit...a Task is what is moved around through the Steps during the Process life-cycle.
 *
 * TODO: Task should very likely be a trait, and several specific types of tasks should be created. Although it is
 * likely going to make each implementation slightly more complex...perhaps...although not as complex as the
 * Step implementation. Hmm....
 */
case class Task(
  id: Option[TaskId] = None,
  processId: ProcessId,
  stepId: StepId,
  title: String,
  description: Option[String] = None,
  assignee: Option[UserId] = None,
  // TODO: add list of candidates...only role based, or include specific users?
  dataRefIdStr: Option[String] = None,
  dataRefTypeStr: Option[String] = None)

object Task extends WithBSONConverters[Task] with WithDateTimeConverters with WithMongo {
  val logger = Logger(classOf[Task])

  implicit val taskReads = Json.reads[Task]
  implicit val taskWrites = Json.writes[Task]

  override implicit def toBSON(t: Task): DBObject = {
    val builder = MongoDBObject.newBuilder
    t.id.foreach(builder += "_id" -> _.asOID)
    builder += "processId" -> t.processId.asOID
    builder += "stepId" -> t.stepId.asOID
    builder += "title" -> t.title
    t.description.foreach(builder += "description" -> _)
    t.assignee.foreach(builder += "assignee" -> _.asOID)
    t.dataRefIdStr.foreach(builder += "dataRefIdStr" -> _)
    t.dataRefTypeStr.foreach(builder += "dataRefTypeStr" -> _)

    builder.result()
  }

  override implicit def fromBSON(dbo: DBObject): Task =
    Task(
      id = TaskId.asOptId(dbo.getAs[ObjectId]("_id")),
      processId = ProcessId.asId(dbo.getAs[ObjectId]("processId").get),
      stepId = StepId.asId(dbo.getAs[ObjectId]("stepId").get),
      title = dbo.getAs[String]("title").get,
      description = dbo.getAs[String]("description"),
      assignee = UserId.asOptId(dbo.getAs[ObjectId]("assignee")),
      dataRefIdStr = dbo.getAs[String]("dataRefIdStr"),
      dataRefTypeStr = dbo.getAs[String]("dataRefTypeStr")
    )

  override val collectionName: String = "tasks"

  // ********************************************************
  // Persistence...
  // ********************************************************

  def save(task: Task) = {
    val res = collection.save(task)

    if (res.isUpdateOfExisting) logger.info(s"Updated existing Task with Id ${res.getUpsertedId}")
    else println(s"Inserted new Task with Id ${res.getUpsertedId}")
  }

  def findById(taskId: TaskId): Option[Task] = collection.findOneByID(taskId.asOID).map(tct => fromBSON(tct))

  def findByProcessId(procId: ProcessId): List[Task] = {
    collection.find(MongoDBObject("processId" -> procId.asOID)).map(Task.fromBSON).toList
  }

}
