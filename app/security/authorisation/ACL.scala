/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import play.api.libs.json.{Format, Json}

case class ACL(id: AuthId, entries: Seq[ACLEntry])

object ACL {
  implicit val f: Format[ACL] = Json.format[ACL]
}