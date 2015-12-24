/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.converters.IdConverters
import models.base.Id
import play.api.libs.json.Format

case class ProjectId(value: String) extends Id

object ProjectId extends IdConverters[ProjectId] {

  implicit val f = Format(reads(ProjectId.apply), writes)

  override implicit def asId(s: String): ProjectId = ProjectId(s)

}