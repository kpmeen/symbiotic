/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import models.core.Username
import models.customer.CustomerId
import models.parties.{OrganizationId, UserId}
import play.api.libs.json.{Format, Json}
import security.authorization.Role

/**
 * Represents a user involvement in a project. The following constraints apply:
 *
 * - 1 user can have >= 1 project membership
 * - 1 membership must have a unique combination of uid + cid + pid + oid
 *
 */
case class Membership(
  id: MembershipId,
  uid: UserId,
  uname: Username,
  cid: CustomerId,
  pid: ProjectId,
  oid: OrganizationId,
  roles: Seq[Role])

object Membership {

  implicit val memFormat: Format[Membership] = Json.format[Membership]

}