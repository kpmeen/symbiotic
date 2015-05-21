/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.WithDBIdConverters
import models.base.DBId

/**
 *
 * @param value
 */
case class ProjectId(value: String) extends DBId

object ProjectId extends WithDBIdConverters[ProjectId] {

  implicit val projectIdReads = reads(ProjectId.apply)
  implicit val projectIdWrites = writes

  override implicit def asId(s: String): ProjectId = ProjectId(s)

}