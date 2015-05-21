/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import java.util.Date

import com.mongodb.casbah.commons.Imports._
import core.converters.WithDateTimeConverters
import models.parties.UserId
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}

object AssignmentDetails {

  case class Assignment(
    id: AssignmentId = AssignmentId.create(),
    assignee: Option[UserId] = None,
    completed: Boolean = false,
    assignedDate: Option[DateTime] = None,
    completionDate: Option[DateTime] = None
    )

  object Assignment extends WithDateTimeConverters {

    implicit val reads: Reads[Assignment] = Json.reads[Assignment]
    implicit val writes: Writes[Assignment] = Json.writes[Assignment]

    def toBSON(a: Assignment): MongoDBObject = {
      val builder = MongoDBObject.newBuilder
      builder += "id" -> a.id.value
      a.assignee.foreach(ass => builder += "assignee" -> ass.asOID)
      builder += "completed" -> a.completed
      a.assignedDate.foreach(ad => builder += "assignedDate" -> ad.toDate)
      a.completionDate.foreach(cd => builder += "completionDate" -> cd.toDate)

      builder.result()
    }

    def fromBSON(dbo: DBObject): Assignment =
      Assignment(
        id = dbo.as[String]("id"),
        assignee = dbo.getAs[ObjectId]("assignee").map(UserId.asId),
        completed = dbo.getAs[Boolean]("completed").getOrElse(false),
        assignedDate = dbo.getAs[Date]("assignedDate").map(asDateTime),
        completionDate = dbo.getAs[Date]("completionDate").map(asDateTime)
      )
  }

}