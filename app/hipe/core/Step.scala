/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.{WithListBSONConverters, WithObjectBSONConverters}
import hipe.core.dsl.TaskStateRule
import play.api.libs.json._

import scala.reflect.ClassTag
import scala.util.Try

/**
 * Defines the basic element of a process. This is the starting point for creating more
 * elaborate building blocks that can be composed together as a Process.
 */
case class Step(
  // TODO: Find a good way to handle sub- processes/steps...
  id: StepId,
  name: String,
  description: Option[String] = None,
  minNumAssignments: Int = 0,
  candidateRoles: Option[Seq[String]] = None,
  transitionRules: Option[Seq[TaskStateRule]] = None)

object Step extends WithObjectBSONConverters[Step] {

  implicit val reads: Reads[Step] = Json.reads[Step]
  implicit val writes: Writes[Step] = Json.writes[Step]

  def toBSON(s: Step): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "id" -> s.id.asOID
    builder += "name" -> s.name
    s.description.foreach(d => builder += "description" -> d)
    builder += "minNumAssignments" -> s.minNumAssignments
    s.candidateRoles.foreach(cr => builder += "candidateRoles" -> cr)
    s.transitionRules.foreach(tr => builder += "transitionRules" -> tr.map(TaskStateRule.toBSON))

    builder.result()
  }

  def fromBSON(dbo: DBObject): Step =
    Step(
      id = StepId.asId(dbo.as[ObjectId]("id")),
      name = dbo.as[String]("name"),
      description = dbo.getAs[String]("description"),
      minNumAssignments = dbo.getAs[Int]("minNumAssignments").getOrElse(0),
      candidateRoles = dbo.getAs[Seq[String]]("candidateRoles"),
      transitionRules = dbo.getAs[Seq[DBObject]]("transitionRules").map(_.map(TaskStateRule.fromBSON))
    )
}

case class StepList(steps: List[Step] = List.empty) {

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

object StepList extends WithListBSONConverters[StepList] {

  def apply[CT: ClassTag](s: Step*): StepList = StepList(s.toList)

  val empty: StepList = List.empty

  implicit val reads: Reads[StepList] = __.read[List[Step]].map(StepList.apply)
  implicit val writes: Writes[StepList] = Writes {
    case sl: StepList => Json.toJson(sl.steps)
  }

  implicit def listToStepList(s: List[Step]): StepList = StepList(s)

  implicit def stepListToList(sl: StepList): List[Step] = sl.steps

  // The below BSON converters cannot be implicit due to the implicit List conversions above.

  override def toBSON(x: StepList): MongoDBList = MongoDBList(x.steps.map(Step.toBSON))

  override def fromBSON(dbo: MongoDBList): StepList =
    StepList(dbo.map(s => Step.fromBSON(s.asInstanceOf[DBObject])).toList)

}

/**
 * Types indicating which steps are surrounding the current Step.
 */
private[hipe] sealed trait SurroundingSteps

private[hipe] case class PrevOrNext(prev: Step, next: Step) extends SurroundingSteps

private[hipe] case class PrevOnly(prev: Step) extends SurroundingSteps

private[hipe] case class NextOnly(next: Step) extends SurroundingSteps