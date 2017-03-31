/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import models.base.BaseMetadata
import models.docmanagement.MetadataKeys._
import models.party.PartyBaseTypes.UserId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ManagedFileMetadata(
    owner: Option[UserId] = None,
    fid: Option[FileId] = None,
    uploadedBy: Option[UserId] = None,
    version: Version = 1,
    isFolder: Option[Boolean] = None,
    path: Option[Path] = None,
    description: Option[String] = None,
    lock: Option[Lock] = None
) extends BaseMetadata

object ManagedFileMetadata {

  implicit val format: Format[ManagedFileMetadata] = (
    (__ \ OwnerKey.key).formatNullable[UserId] and
      (__ \ FidKey.key).formatNullable[FileId] and
      (__ \ UploadedByKey.key).formatNullable[UserId] and
      (__ \ VersionKey.key).format[Version] and
      (__ \ IsFolderKey.key).formatNullable[Boolean] and
      (__ \ PathKey.key).formatNullable[Path] and
      (__ \ DescriptionKey.key).formatNullable[String] and
      (__ \ LockKey.key).formatNullable[Lock]
  )(ManagedFileMetadata.apply, unlift(ManagedFileMetadata.unapply))

}
