/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import models.party.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data.BaseMetadata

case class AvatarMetadata(uid: UserId) extends BaseMetadata
