/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import models.parties.UserId
import play.api.libs.json.{Json, Format}

// TODO: principal should be of a type Principal that is extended by users and groups/roles
case class ACLEntry(principal: UserId, permissions: Set[Permission])

object ACLEntry {
  implicit val f: Format[ACLEntry] = Json.format[ACLEntry]
}