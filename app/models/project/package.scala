/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models

import core.converters.IdConverters
import models.base.Id

package object project {

  case class ProjectId(value: String) extends Id

  object ProjectId extends IdConverters[ProjectId] {

    implicit val projectIdReads = reads(ProjectId.apply)
    implicit val projectIdWrites = writes

    override implicit def asId(s: String): ProjectId = ProjectId(s)

  }

  /**
   * Id type for project membership
   */
  case class MembershipId(value: String) extends Id

  object MembershipId extends IdConverters[MembershipId] {

    implicit val membershipIdReads = reads(MembershipId.apply)
    implicit val membershipIdWrites = writes

    override implicit def asId(s: String): MembershipId = MembershipId(s)

  }

}
