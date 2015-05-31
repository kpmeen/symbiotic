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
 * @param elements a List of StepGroup instances.
 */
case class StepGroupList(elements: List[StepGroup] = List.empty) {
  def flatten: StepList = elements.flatMap(_.steps)

  def findBy(sid: StepId): Option[StepGroup] = elements.find(_.steps.exists(_.id.contains(sid)))

  def findByWithPosition(sid: StepId): Option[(StepGroup, Int)] =
    elements.zipWithIndex.find(_._1.steps.exists(_.id.contains(sid)))

  def findStep(sid: StepId): Option[(StepGroup, Step)] =
    findBy(sid).flatMap(sg => sg.steps.findStep(sid).map((sg, _)))

  def findWithPosition(sgid: StepGroupId): Option[(StepGroup, Int)] = elements.zipWithIndex.find(_._1.id.contains(sgid))

  def findFull(sid: StepId): Option[(StepGroup, Int, Step, Int)] = {
    for {
      g <- findByWithPosition(sid)
      s <- g._1.steps.findWithPosition(sid)
    } yield {
      (g._1, g._2, s._1, s._2)
    }
  }

  def nextStepFrom(stepId: StepId): Option[Step] = flatten.nextFrom(stepId)

  def previousStepFrom(stepId: StepId): Option[Step] = flatten.previousFrom(stepId)

  def insert(sg: StepGroup, pos: Int): StepGroupList = {
    val lr = elements.splitAt(pos)
    lr._1 ::: StepGroupList(sg) ::: lr._2
  }

  def remove(pos: Int): StepGroupList = {
    val lr = elements.splitAt(pos)
    lr._1 ::: lr._2.tail
  }

  def move(currPos: Int, newPos: Int): StepGroupList = {
    val index = if (newPos >= elements.length) elements.length - 1 else newPos
    val lr = elements.splitAt(currPos)
    val removed = (lr._1 ::: lr._2.tail).splitAt(index)
    removed._1 ::: StepGroupList(elements(currPos)) ::: removed._2
  }
}

object StepGroupList extends ListBSONConverters[StepGroupList] {

  def apply[CT: ClassTag](s: StepGroup*): StepGroupList = StepGroupList(s.toList)

  val empty: StepGroupList = List.empty

  implicit val reads: Reads[StepGroupList] = __.read[List[StepGroup]].map(StepGroupList.apply)

  implicit val writes: Writes[StepGroupList] = Writes {
    case sl: StepGroupList => Json.toJson(sl.elements)
  }

  implicit def listToStepGroupList(s: List[StepGroup]): StepGroupList = StepGroupList(s)

  implicit def stepGroupListToList(sl: StepGroupList): List[StepGroup] = sl.elements

  override def toBSON(x: StepGroupList): Seq[DBObject] = x.elements.map(StepGroup.toBSON)

  override def fromBSON(dbo: MongoDBList): StepGroupList =
    StepGroupList(dbo.map(s => StepGroup.fromBSON(s.asInstanceOf[DBObject])).toList)

}