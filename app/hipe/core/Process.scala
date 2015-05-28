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
 * @param stepList List of Steps in the process.
 */
case class Process(
  _id: Option[ObjectId] = None,
  id: Option[ProcessId] = None,
  name: String,
  strict: Boolean = false,
  description: Option[String] = None,
  stepList: StepList = StepList.empty) extends PersistentType {

  def step(id: StepId): Option[Step] = stepList.find(_.id.contains(id))

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
      (__ \ "steps").format[StepList]
    )(Process.apply, unlift(Process.unapply))

  implicit override def toBSON(x: Process): DBObject = {
    val builder = MongoDBObject.newBuilder

    x._id.foreach(builder += "_id" -> _)
    x.id.foreach(builder += "id" -> _.value)
    builder += "name" -> x.name
    builder += "strict" -> x.strict
    x.description.foreach(builder += "description" -> _)
    builder += "steps" -> StepList.toBSON(x.stepList)

    builder.result()
  }

  override def fromBSON(dbo: DBObject): Process = {
    Process(
      _id = dbo.getAs[ObjectId]("_id"),
      id = dbo.getAs[String]("id"),
      name = dbo.as[String]("name"),
      strict = dbo.getAs[Boolean]("strict").getOrElse(false),
      description = dbo.getAs[String]("description"),
      stepList = StepList.fromBSON(dbo.as[MongoDBList]("steps"))
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