/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import core.security.authorisation.Role
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, PersistentTypeConverters, Username}
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import play.api.libs.json.{Format, Json}

/**
 * Represents a user involvement in a project. The following constraints apply:
 *
 * - 1 user can have >= 1 project membership
 * - 1 membership must have a unique combination of uid + orgId + pid
 *
 */
case class Member(
  id: Option[MemberId],
  v: Option[VersionStamp],
  uid: UserId,
  uname: Username,
  orgId: OrganisationId,
  pid: ProjectId,
  represents: Option[OrganisationId] = None,
  roles: Seq[Role] = Seq.empty[Role]
) extends PersistentType

object Member extends PersistentTypeConverters {

  implicit val memFormat: Format[Member] = Json.format[Member]

}