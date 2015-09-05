/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import models.parties.UserId
import play.api.libs.json.{Json, Format}

case class ACLEntry(principal: UserId, permissions: Seq[Permission])

object ACLEntry {
  implicit val f: Format[ACLEntry] = Json.format[ACLEntry]
}