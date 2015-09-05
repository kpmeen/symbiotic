/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import converters.IdConverters
import models.base.Id
import play.api.libs.json.{Reads, Writes}

case class AclId(value: String) extends Id

object AclId extends IdConverters[AclId] {

  implicit val r: Reads[AclId] = reads(AclId.apply)
  implicit val w: Writes[AclId] = writes

  override implicit def asId(s: String): AclId = AclId(s)
}