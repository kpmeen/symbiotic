/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import models.base.BaseMetadata
import models.party.PartyBaseTypes.UserId

case class AvatarMetadata(
  uid: UserId
) extends BaseMetadata