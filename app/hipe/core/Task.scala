/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.{HipeDB, WithMongoIndex}
import hipe.core.States.TaskState
import models.base.PersistentType.{UserStamp, VersionStamp}
import models.base.{PersistentType, PersistentTypeConverters}
import models.parties.UserId
import org.slf4j.LoggerFactory
import play.api.libs.json.{Format, Json}

/**
 * The interesting bit...a Task is what is moved around through the
 * Steps during the Process life-cycle.
 */
case class Task(
  v: Option[VersionStamp] = None,
  id: Option[TaskId] = None,
  processId: ProcessId,
  stepId: StepId,
  title: String,
  description: Option[String] = None,
  state: TaskState,
  assignments: Seq[Assignment] = Seq.empty,
  dataRef: Option[TaskDataRef] = None) extends PersistentType {

  private[hipe] def updateAssignment(func: (Seq[Assignment]) => Option[Assignment]): Seq[Assignment] = {
    val maybeAssigns = func(assignments)
    maybeAssigns.map { a =>
      val origIndex = assignments.indexWhere(_.id == a.id)
      assignments.updated(origIndex, a)
    }.getOrElse(assignments)
  }

  private[hipe] def isTaskCompleted(currStep: Step): Boolean =
    assignments.count(_.completed == true) >= currStep.minCompleted

  private[hipe] def initAssignmentsFor(step: Step): Task =
    this.copy(stepId = step.id.get, assignments = Assignment.createAssignments(step.minAssignments))

  private[hipe] def addAssignmentFor(step: Step): Task =
    this.copy(assignments = assignments ++ Assignment.createAssignments(1))

  private[hipe] def assignmentApply(cond: Task => Boolean, cp: Seq[Assignment] => Option[Assignment]): Option[Task] =
    if (cond(this)) Some(this.copy(assignments = updateAssignment(ass => cp(ass)))) else None

  private[hipe] def incrementVersion(by: UserId, origVersion: Option[VersionStamp]): (UserStamp, Task) = {
    val us = UserStamp.create(by)
    (us, copy(v = origVersion.map(ov => ov.copy(version = ov.version + 1, modified = Some(us)))))
  }

  private[hipe] def findAssignmentForUser(uid: UserId): Option[Assignment] =
    assignments.find(_.assignee.contains(uid))

}

object Task extends PersistentTypeConverters with ObjectBSONConverters[Task] with DateTimeConverters with HipeDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(classOf[Task])

  implicit val taskReads = Json.reads[Task]
  implicit val taskWrites = Json.writes[Task]

  override val collectionName: String = "hipe.tasks"

  implicit override def toBSON(t: Task): DBObject = {
    val builder = MongoDBObject.newBuilder
    t.v.foreach(builder += "v" -> VersionStamp.toBSON(_))
    t.id.foreach(builder += "id" -> _.value)
    builder += "processId" -> t.processId.value
    builder += "stepId" -> t.stepId.value
    builder += "title" -> t.title
    t.description.foreach(builder += "description" -> _)
    builder += "state" -> TaskState.asString(t.state)
    builder += "assignments" -> t.assignments.map(Assignment.toBSON)
    t.dataRef.foreach(builder += "dataRef" -> TaskDataRef.toBSON(_))

    builder.result()
  }

  implicit override def fromBSON(dbo: DBObject): Task =
    Task(
      v = dbo.getAs[DBObject]("v").map(VersionStamp.fromBSON),
      id = dbo.getAs[String]("id"),
      processId = dbo.as[String]("processId"),
      stepId = dbo.as[String]("stepId"),
      title = dbo.as[String]("title"),
      description = dbo.getAs[String]("description"),
      state = dbo.as[String]("state"),
      assignments = dbo.getAs[Seq[DBObject]]("assignments").map(_.map(Assignment.fromBSON)).getOrElse(Seq.empty),
      dataRef = dbo.getAs[DBObject]("dataRef").map(TaskDataRef.fromBSON)
    )

  override def ensureIndex(): Unit = index(List("id", "processId", "stepId"), collection)

  def findById(taskId: TaskId): Option[Task] =
    collection.findOne(
      o = MongoDBObject("id" -> taskId.value),
      orderBy = MongoDBObject("v.version" -> -1)
    ).map(tct => fromBSON(tct))

  def findAllVersions(taskId: TaskId): List[Task] =
    collection.find(MongoDBObject(
      "id" -> taskId.value
    )).sort(MongoDBObject("v.version" -> -1)).map(fromBSON).toList

  def findByProcessId(procId: ProcessId): List[Task] =
    collection.find(MongoDBObject("processId" -> procId.value)).map(Task.fromBSON).toList

  def save(task: Task) = {
    collection.findAndModify(
      query = MongoDBObject("id" -> task.id.get.value),
      sort = MongoDBObject.empty,
      fields = MongoDBObject.empty,
      update = task,
      remove = false,
      returnNew = true,
      upsert = true
    ).fold(logger.error(s"An error occured trying to save task: $task"))(t => logger.debug(s"Saved task: $t"))
  }

}

case class TaskDataRef(refId: String, refType: String)

object TaskDataRef extends ObjectBSONConverters[TaskDataRef] {
  implicit val format: Format[TaskDataRef] = Json.format[TaskDataRef]

  override def toBSON(x: TaskDataRef): DBObject =
    MongoDBObject("refId" -> x.refId, "refType" -> x.refType)

  override def fromBSON(dbo: DBObject): TaskDataRef =
    TaskDataRef(dbo.as[String]("refId"), dbo.as[String]("refType"))
}
