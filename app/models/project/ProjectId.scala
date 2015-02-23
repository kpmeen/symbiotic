/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import models.core.{Id, WithIdTransformers}
import org.bson.types.ObjectId

case class ProjectId(id: ObjectId) extends Id

object ProjectId extends WithIdTransformers {

  implicit val projectIdReads = reads[ProjectId](ProjectId.apply)
  implicit val projectIdWrites = writes[ProjectId]

  def fromString(pid: String): Option[ProjectId] = Option(new ObjectId(pid)).flatMap(oid => Option(ProjectId(oid)))

}
