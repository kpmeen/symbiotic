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
 * TODO: Task should very likely be a trait, and several specific types of tasks should be created.
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
    t.id.foreach(builder += "_id" -> _.id)
    builder += "processId" -> t.processId.id
    builder += "stepId" -> t.stepId.id
    t.description.foreach(builder += "description" -> _)
    t.assignee.foreach(builder += "assignee" -> _.id)
    t.dataRefIdStr.foreach(builder += "dataRefIdStr" -> _)
    t.dataRefTypeStr.foreach(builder += "dataRefTypeStr" -> _)

    builder.result()
  }

  override implicit def fromBSON(dbo: DBObject): Task =
    Task(
      id = dbo.getAs[ObjectId]("_id"),
      processId = dbo.getAs[ObjectId]("processId").get,
      stepId = dbo.getAs[ObjectId]("stepId").get,
      title = dbo.getAs[String]("title").get,
      description = dbo.getAs[String]("description"),
      assignee = dbo.getAs[ObjectId]("assignee"),
      dataRefIdStr = dbo.getAs[String]("dataRefIdStr"),
      dataRefTypeStr = dbo.getAs[String]("dataRefTypeStr")
    )

  override val collectionName: String = "tasks"

  // TODO: Implement mongodb integration here...
  def save(task: Task) = {
    val res = collection.save(task)

    if (res.isUpdateOfExisting) logger.info("Updated existing user")
    else logger.info("Inserted new user")
  }

  def findById(taskId: TaskId): Option[Task] = collection.findOneByID(taskId.id).map(tct => fromBSON(tct))

}
