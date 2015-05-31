/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import java.util.Date

import com.mongodb.casbah.commons.Imports._
import core.converters.DateTimeConverters
import hipe.core.States.AssignmentState
import hipe.core.States.AssignmentStates._
import models.parties.UserId
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}

case class Assignment(
  id: AssignmentId = AssignmentId.create(),
  assignee: Option[UserId] = None,
  status: AssignmentState = Available(),
  assignedDate: Option[DateTime] = None,
  completionDate: Option[DateTime] = None) {

  def completed: Boolean = {
    status match {
      case Completed() | Aborted() => true
      case Available() | Assigned() => false
    }
  }

}

object Assignment extends DateTimeConverters {

  implicit val reads: Reads[Assignment] = Json.reads[Assignment]
  implicit val writes: Writes[Assignment] = Json.writes[Assignment]

  def toBSON(a: Assignment): MongoDBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "id" -> a.id.value
    a.assignee.foreach(ass => builder += "assignee" -> ass.value)
    builder += "status" -> AssignmentState.asString(a.status)
    a.assignedDate.foreach(ad => builder += "assignedDate" -> ad.toDate)
    a.completionDate.foreach(cd => builder += "completionDate" -> cd.toDate)

    builder.result()
  }

  def fromBSON(dbo: DBObject): Assignment =
    Assignment(
      id = dbo.as[String]("id"),
      assignee = dbo.getAs[String]("assignee"),
      status = dbo.getAs[String]("status").map(AssignmentState.asState).getOrElse(Available()),
      assignedDate = dbo.getAs[Date]("assignedDate").map(asDateTime),
      completionDate = dbo.getAs[Date]("completionDate").map(asDateTime)
    )
}