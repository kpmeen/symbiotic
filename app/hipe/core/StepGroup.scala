/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.{ListBSONConverters, ObjectBSONConverters}
import hipe.core.States.{TaskState, TaskStates}
import play.api.libs.json._

import scala.reflect.ClassTag

/**
 * The StepGroup is one of the core building blocks in the building of a Process.
 * Its main property is a StepList containing 1 or more Step instances.
 *
 * @param id Optional StepGroupId. If not set, a new one will be generated when saving.
 * @param name Optional name
 * @param priv indicates if the content of steps is restricted to certain users/groups
 * @param steps StepList containing the actual steps.
 */
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

  def create(step: Step*): StepGroup = StepGroup(id = StepGroupId.createOpt(), steps = StepList(step.toList))

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

/**
 * This class wraps a {{{List[StepGroup]}}} and is essentially a custom List implementation.
 *
 * It provides a usefull set of functions/methods for interacting with the list content
 *
 * @param groups a List of StepGroup instances.
 */
case class StepGroupList(groups: List[StepGroup] = List.empty) {
  def flatten: StepList = groups.flatMap(_.steps)

  def findWithStep(stepId: StepId): Option[StepGroup] =
    groups.find(_.steps.exists(_.id.contains(stepId)))

  def findStep(stepId: StepId): Option[(StepGroup, Step)] =
    findWithStep(stepId).flatMap { sg =>
      sg.steps.find(_.id.contains(stepId)).map((sg, _))
    }

  def findWithIndex(stepGroupId: StepGroupId): Option[(StepGroup, Int)] =
    groups.zipWithIndex.find(_._1.id.contains(stepGroupId))

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