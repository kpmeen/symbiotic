/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.{ListBSONConverters, ObjectBSONConverters}
import hipe.core.dsl.TaskStateRule
import play.api.libs.json._

import scala.reflect.ClassTag
import scala.util.Try

/**
 * Defines the basic element of a process. This is the starting point for creating more
 * elaborate building blocks that can be composed together as a Process.
 */
case class Step(
  id: Option[StepId] = None,
  name: String,
  description: Option[String] = None,
  minAssignments: Int = 1,
  minCompleted: Int = 0,
  candidateRoles: Option[Seq[String]] = None,
  transitionRules: Option[Seq[TaskStateRule]] = None,
  autoTransition: Boolean = false)

object Step extends ObjectBSONConverters[Step] {

  implicit val format: Format[Step] = Json.format[Step]

  def toBSON(s: Step): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "id" -> s.id.getOrElse(StepId.create()).value
    builder += "name" -> s.name
    s.description.foreach(d => builder += "description" -> d)
    builder += "minAssignments" -> s.minAssignments
    builder += "minCompleted" -> s.minCompleted
    s.candidateRoles.foreach(cr => builder += "candidateRoles" -> cr)
    s.transitionRules.foreach(tr => builder += "transitionRules" -> tr.map(TaskStateRule.toBSON))
    builder += "autoTransition" -> s.autoTransition

    builder.result()
  }

  def fromBSON(dbo: DBObject): Step =
    Step(
      id = StepId.asOptId(dbo.as[String]("id")),
      name = dbo.as[String]("name"),
      description = dbo.getAs[String]("description"),
      minAssignments = dbo.getAs[Int]("minAssignments").getOrElse(1),
      minCompleted = dbo.getAs[Int]("minCompleted").getOrElse(0),
      candidateRoles = dbo.getAs[Seq[String]]("candidateRoles"),
      transitionRules = dbo.getAs[Seq[DBObject]]("transitionRules").map(_.map(TaskStateRule.fromBSON)),
      autoTransition = dbo.getAs[Boolean]("autoTransition").getOrElse(false)
    )
}

case class StepList(elements: List[Step] = List.empty) {

  def nextFrom(stepId: StepId): Option[Step] = {
    elements.zipWithIndex.find(z => z._1.id.contains(stepId)).flatMap { s =>
      Try {
        val next = elements(s._2 + 1)
        Option(next)
      }.recover {
        case _ => None
      }.get
    }
  }

  def previousFrom(stepId: StepId): Option[Step] = {
    elements.zipWithIndex.find(z => z._1.id.contains(stepId)).flatMap { s =>
      Try {
        val prev = elements(s._2 - 1)
        Option(prev)
      }.recover {
        case t: Throwable => None
      }.get
    }
  }

  def findStep(stepId: StepId): Option[Step] = elements.find(_.id.contains(stepId))

  def findWithPosition(stepId: StepId): Option[(Step, Int)] = elements.zipWithIndex.find(_._1.id.contains(stepId))

  def insert(sg: Step, pos: Int): StepList = {
    val lr = elements.splitAt(pos)
    lr._1 ::: StepList(sg) ::: lr._2
  }

  def remove(pos: Int): StepList = {
    val lr = elements.splitAt(pos)
    lr._1 ::: lr._2.tail
  }

  def move(currPos: Int, newPos: Int): StepList = {
    val index = if (newPos >= elements.length) elements.length - 1 else newPos
    val lr = elements.splitAt(currPos)
    val removed = (lr._1 ::: lr._2.tail).splitAt(index)
    removed._1 ::: StepList(elements(currPos)) ::: removed._2
  }

}

object StepList extends ListBSONConverters[StepList] {

  def apply[CT: ClassTag](s: Step*): StepList = StepList(s.toList)

  val empty: StepList = List.empty

  implicit val reads: Reads[StepList] = __.read[List[Step]].map(StepList.apply)
  implicit val writes: Writes[StepList] = Writes {
    case sl: StepList => Json.toJson(sl.elements)
  }

  implicit def listToStepList(s: List[Step]): StepList = StepList(s)

  implicit def stepListToList(sl: StepList): List[Step] = sl.elements

  // ============================================================
  // The below BSON converters cannot be implicit due
  // to the conflicting implicit List conversions above.
  // ============================================================

  override def toBSON(x: StepList): Seq[DBObject] = x.elements.map(Step.toBSON)

  override def fromBSON(dbo: MongoDBList): StepList =
    StepList(dbo.map(s => Step.fromBSON(s.asInstanceOf[DBObject])).toList)

}