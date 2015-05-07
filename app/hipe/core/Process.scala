/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.{WithBSONConverters, WithDateTimeConverters}
import core.mongodb.WithMongo
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

/**
 * This case class holds actual process configuration.
 *
 * @param id ProcessId The unique identifier for the Process
 * @param name String with a readable name
 * @param strict Boolean flag indicating if movement of tasks in the process should be free-form/open or restricted
 * @param description String Readable text describing the process
 * @param steps List of Steps in the process.
 */
case class Process(
  id: Option[ProcessId],
  name: String,
  strict: Boolean = false,
  description: Option[String] = None,
  steps: List[Step] = List.empty) {

  def nextStepFrom(stepId: StepId): Option[Step] = {
    steps.zipWithIndex.find(z => z._1.id == stepId).flatMap { s =>
      Try {
        val next = steps(s._2 + 1)
        Option(next)
      }.recover {
        case _ => None
      }.get
    }
  }

  def previousStepFrom(stepId: StepId): Option[Step] = {
    steps.zipWithIndex.find(z => z._1.id == stepId).flatMap { s =>
      Try {
        val prev = steps(s._2 - 1)
        Option(prev)
      }.recover {
        case t: Throwable => None
      }.get
    }
  }
}

/**
 * The companion, with JSON and BSON converters for exposure and persistence.
 */
object Process extends WithBSONConverters[Process] with WithDateTimeConverters with WithMongo {

  val logger = Logger(classOf[Process])

  implicit val procFormat: Format[Process] = (
    (__ \ "id").formatNullable[ProcessId] and
      (__ \ "name").format[String] and
      (__ \ "strict").format[Boolean] and
      (__ \ "description").formatNullable[String] and
      (__ \ "steps").format[List[Step]]
    )(Process.apply, unlift(Process.unapply))

  override implicit def toBSON(x: Process): DBObject = {
    val builder = MongoDBObject.newBuilder

    x.id.foreach(builder += "_id" -> _.asOID)
    builder += "name" -> x.name
    builder += "strict" -> x.strict
    x.description.foreach(builder += "description" -> _)
    builder += "steps" -> x.steps.map(Step.toBSON)

    builder.result()
  }

  override implicit def fromBSON(dbo: DBObject): Process = {
    Process(
      id = ProcessId.asOptId(dbo.getAs[ObjectId]("_id")),
      name = dbo.as[String]("name"),
      strict = dbo.getAs[Boolean]("strict").getOrElse(false),
      description = dbo.getAs[String]("description"),
      steps = dbo.as[MongoDBList]("steps").map(s => Step.fromBSON(s.asInstanceOf[DBObject])).toList
    )
  }

  override val collectionName: String = "processes"

  def save(proc: Process): Unit = {
    val res = collection.save(proc)

    if (res.isUpdateOfExisting) logger.info("Updated existing user")
    else logger.info("Inserted new user")

    logger.debug(res.toString)
  }

  def findById(procId: ProcessId): Option[Process] = {
    collection.findOneByID(procId.asOID).map(pct => fromBSON(pct))
  }

  def delete(procId: ProcessId): Unit = {
    collection.remove(MongoDBObject("_id" -> procId.asOID))
  }

}