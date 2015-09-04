/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import converters.IdConverters
import models.base.Id
import play.api.libs.json.{Reads, Writes}

case class AuthId(value: String) extends Id

object AuthId extends IdConverters[AuthId] {

  implicit val r: Reads[AuthId] = reads(AuthId.apply)
  implicit val w: Writes[AuthId] = writes

  override implicit def asId(s: String): AuthId = AuthId(s)
}