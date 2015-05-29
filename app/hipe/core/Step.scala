/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.{ListBSONConverters, ObjectBSONConverters}
import hipe.core.States.{TaskState, TaskStates}
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
  transitionRules: Option[Seq[TaskStateRule]] = None)

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
      transitionRules = dbo.getAs[Seq[DBObject]]("transitionRules").map(_.map(TaskStateRule.fromBSON))
    )
}

case class StepGroupList(groups: List[StepGroup] = List.empty) {
  def flatten: StepList = groups.flatMap(_.steps)

  def findWithStep(stepId: StepId): Option[StepGroup] =
    groups.find(_.steps.exists(_.id.contains(stepId)))

  def findStep(stepId: StepId): Option[(StepGroup, Step)] =
    findWithStep(stepId).flatMap { sg =>
      sg.steps.find(_.id.contains(stepId)).map((sg, _))
    }

  def nextStepFrom(stepId: StepId): Option[Step] = flatten.nextFrom(stepId)

  def previousStepFrom(stepId: StepId): Option[Step] = flatten.previousFrom(stepId)
}

object StepGroupList extends ListBSONConverters[StepGroupList] {

  def apply[CT: ClassTag](s: StepGroup*): StepGroupList = StepGroupList(s.toList)

  val empty: StepGroupList = List.empty

  implicit val reads: Reads[StepGroupList] = __.read[List[StepGroup]].map(StepGroupList.apply)

  implicit val writes: Writes[StepGroupList] = Writes {
    case sl: StepGroupList => Json.toJson(sl.groups)
  }

  implicit def listToStepGroupList(s: List[StepGroup]): StepGroupList = StepGroupList(s)

  implicit def stepGroupListToList(sl: StepGroupList): List[StepGroup] = sl.groups

  override def toBSON(x: StepGroupList): Seq[DBObject] = x.groups.map(StepGroup.toBSON)

  override def fromBSON(dbo: MongoDBList): StepGroupList =
    StepGroupList(dbo.map(s => StepGroup.fromBSON(s.asInstanceOf[DBObject])).toList)

}

case class StepGroup(
  id: Option[StepGroupId],
  name: Option[String] = None,
  priv: Boolean = false,
  steps: StepList = StepList.empty) {

  def visibleState: Option[TaskState] = {
    if (priv) Some(TaskStates.InProgress()) else None
  }
}

object StepGroup extends ObjectBSONConverters[StepGroup] {

  def create(step: Step*): StepGroup = StepGroup(id = StepGroupId.createOpt, steps = StepList(step.toList))

  implicit val format: Format[StepGroup] = Json.format[StepGroup]

  override def toBSON(sg: StepGroup): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "id" -> sg.id.getOrElse(StepGroupId.create()).value
    sg.name.foreach(builder += "name" -> _)
    builder += "priv" -> sg.priv
    builder += "steps" -> StepList.toBSON(sg.steps)

    builder.result()
  }

  override def fromBSON(dbo: DBObject): StepGroup =
    StepGroup(
      id = StepGroupId.asOptId(dbo.getAs[String]("id")),
      name = dbo.getAs[String]("name"),
      priv = dbo.as[Boolean]("priv"),
      steps = StepList.fromBSON(dbo.as[MongoDBList]("steps"))
    )
}

case class StepList(steps: List[Step] = List.empty) {

  def nextFrom(stepId: StepId): Option[Step] = {
    steps.zipWithIndex.find(z => z._1.id.contains(stepId)).flatMap { s =>
      Try {
        val next = steps(s._2 + 1)
        Option(next)
      }.recover {
        case _ => None
      }.get
    }
  }

  def previousFrom(stepId: StepId): Option[Step] = {
    steps.zipWithIndex.find(z => z._1.id.contains(stepId)).flatMap { s =>
      Try {
        val prev = steps(s._2 - 1)
        Option(prev)
      }.recover {
        case t: Throwable => None
      }.get
    }
  }

}

object StepList extends ListBSONConverters[StepList] {

  def apply[CT: ClassTag](s: Step*): StepList = StepList(s.toList)

  val empty: StepList = List.empty

  implicit val reads: Reads[StepList] = __.read[List[Step]].map(StepList.apply)
  implicit val writes: Writes[StepList] = Writes {
    case sl: StepList => Json.toJson(sl.steps)
  }

  implicit def listToStepList(s: List[Step]): StepList = StepList(s)

  implicit def stepListToList(sl: StepList): List[Step] = sl.steps

  // The below BSON converters cannot be implicit due to the implicit List conversions above.

  override def toBSON(x: StepList): Seq[DBObject] = x.steps.map(Step.toBSON)

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