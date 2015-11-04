/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import com.mongodb.casbah.Imports._
import models.base.{BaseMetadataConverter, BaseMetadata}
import models.party.PartyBaseTypes.UserId

case class AvatarMetadata(
  uid: UserId
) extends BaseMetadata

object AvatarMetadata extends BaseMetadataConverter[AvatarMetadata] {
  override implicit def toBSON(amd: AvatarMetadata): DBObject =
    MongoDBObject("uid" -> amd.uid.value)

  override implicit def fromBSON(dbo: DBObject): AvatarMetadata =
    AvatarMetadata(UserId.asId(dbo.as[String]("uid")))
}