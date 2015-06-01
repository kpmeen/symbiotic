/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.{DateTimeConverters, ObjectBSONConverters}
import core.mongodb.{SymbioticDB, WithMongoIndex}
import models.base.{PersistentType, PersistentTypeConverters}
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * This case class holds actual process configuration.
 *
 * @param id ProcessId The unique identifier for the Process
 * @param name String with a readable name
 * @param strict Boolean flag indicating if movement of tasks in the process should be free-form/open or restricted
 * @param description String Readable text describing the process
 * @param stepGroups List of Steps in the process.
 */
case class Process(
  _id: Option[ObjectId] = None,
  id: Option[ProcessId] = None,
  name: String,
  strict: Boolean = false,
  description: Option[String] = None,
  stepGroups: StepGroupList = StepGroupList.empty) extends PersistentType {

  def step(sid: StepId): Option[Step] = stepGroups.flatten.findStep(sid)

  def removeStep(group: StepGroup, step: Step): Process = {
    val grpPos = stepGroups.indexWhere(_.id == group.id)
    if (group.steps.size == 1) {
      this.copy(stepGroups = stepGroups.remove(grpPos))
    } else {
      val stepPos = group.steps.indexWhere(_.id == step.id)
      val grp = group.copy(steps = group.steps.remove(stepPos))
      this.copy(stepGroups = stepGroups.updated(grpPos, grp))
    }
  }

  /**
   * Calculates the surroundings for the current Step. Most useful for a "strict" process
   *
   * @param currStep the current StepId
   * @return a type of PrevNextStepType that may or may not have previous and/or next Step references.
   */
  private[hipe] def prevNextSteps(currStep: StepId): SurroundingSteps = {
    val steps = stepGroups.flatten
    val currPos = steps.indexWhere(_.id.contains(currStep))

    if (currPos == 0) {
      NextOnly(steps(1))
    } else if (currPos == steps.length - 1) {
      PrevOnly(steps(steps.length - 2))
    } else {
      PrevOrNext(steps(currPos - 1), steps(currPos + 1))
    }
  }

}

/**
 * The companion, with JSON and BSON converters for exposure and persistence.
 */
object Process extends PersistentTypeConverters with ObjectBSONConverters[Process] with DateTimeConverters with SymbioticDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(classOf[Process])

  implicit val procFormat: Format[Process] = (
    (__ \ "_id").formatNullable[ObjectId] and
      (__ \ "id").formatNullable[ProcessId] and
      (__ \ "name").format[String] and
      (__ \ "strict").format[Boolean] and
      (__ \ "description").formatNullable[String] and
      (__ \ "stepGroups").format[StepGroupList]
    )(Process.apply, unlift(Process.unapply))

  implicit override def toBSON(x: Process): DBObject = {
    val builder = MongoDBObject.newBuilder

    x._id.foreach(builder += "_id" -> _)
    x.id.foreach(builder += "id" -> _.value)
    builder += "name" -> x.name
    builder += "strict" -> x.strict
    x.description.foreach(builder += "description" -> _)
    builder += "stepGroups" -> StepGroupList.toBSON(x.stepGroups)

    builder.result()
  }

  override def fromBSON(dbo: DBObject): Process = {
    Process(
      _id = dbo.getAs[ObjectId]("_id"),
      id = dbo.getAs[String]("id"),
      name = dbo.as[String]("name"),
      strict = dbo.getAs[Boolean]("strict").getOrElse(false),
      description = dbo.getAs[String]("description"),
      stepGroups = StepGroupList.fromBSON(dbo.as[MongoDBList]("stepGroups"))
    )
  }

  override val collectionName: String = "processes"

  override def ensureIndex(): Unit = ???

  def save(proc: Process): Unit = {
    val res = collection.save(proc)

    if (logger.isDebugEnabled) {
      if (res.isUpdateOfExisting) logger.debug(s"Updated existing Process ${proc.id}")
      else logger.debug("Inserted new Process")

      logger.debug(res.toString)
    }
  }

  def findById(procId: ProcessId): Option[Process] = {
    collection.findOne(MongoDBObject("id" -> procId.value)).map(pct => fromBSON(pct))
  }

  def delete(procId: ProcessId): Unit = {
    collection.remove(MongoDBObject("id" -> procId.value))
  }
}