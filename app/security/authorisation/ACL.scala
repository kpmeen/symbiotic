/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import converters.IdConverters
import models.base.Id
import play.api.libs.json.{Format, Json}

case class ACL(id: AuthId, entries: Seq[ACLEntry])

object ACL {
  implicit val f: Format[ACL] = Json.format[ACL]
}

case class ACLEntry(principal: Principal, permissions: Seq[Permission])

object ACLEntry {
  implicit val f: Format[ACLEntry] = Json.format[ACLEntry]
}

case class AuthId(value: String) extends Id

object AuthId extends IdConverters[AuthId] {

  implicit val r = reads(AuthId.apply)
  implicit val w = writes

  override implicit def asId(s: String): AuthId = AuthId(s)
}