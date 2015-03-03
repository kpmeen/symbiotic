/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.WithIdConverters
import models.base.Id
import org.bson.types.ObjectId

case class ProjectId(id: ObjectId) extends Id

object ProjectId extends WithIdConverters[ProjectId] {

  implicit val projectIdReads = reads(ProjectId.apply)
  implicit val projectIdWrites = writes

  override implicit def asId(oid: ObjectId): ProjectId = ProjectId(oid)

  def fromString(pid: String): Option[ProjectId] = Option(new ObjectId(pid)).flatMap(oid => Option(ProjectId(oid)))

}
