/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import play.api.libs.json.{Json, Format}

case class ACLEntry(principal: Principal, permissions: Seq[Permission])

object ACLEntry {
  implicit val f: Format[ACLEntry] = Json.format[ACLEntry]
}