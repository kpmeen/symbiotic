/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.WithIdConverters
import models.base.Id
import org.bson.types.ObjectId

/**
 *
 * @param id
 */
case class ProjectId(id: ObjectId) extends Id

object ProjectId extends WithIdConverters[ProjectId] {

  implicit val projectIdReads = reads(ProjectId.apply)
  implicit val projectIdWrites = writes

  override implicit def asId(oid: ObjectId): ProjectId = ProjectId(oid)

  override implicit def asId(s: String): ProjectId = ProjectId(new ObjectId(s))

}