/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.WithIdConverters
import models.base.Id

/**
 *
 * @param id
 */
case class ProjectId(id: String) extends Id

object ProjectId extends WithIdConverters[ProjectId] {

  implicit val projectIdReads = reads(ProjectId.apply)
  implicit val projectIdWrites = writes

  override implicit def asId(s: String): ProjectId = ProjectId(s)

}