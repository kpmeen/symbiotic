/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import core.security.authorisation.ACL
import models.base.BaseMetadata
import models.docmanagement.MetadataKeys._
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.project.ProjectId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ManagedFileMetadata(
  oid: OrganisationId,
  pid: Option[ProjectId] = None,
  fid: Option[FileId] = None,
  uploadedBy: Option[UserId] = None,
  version: Version = 1,
  isFolder: Option[Boolean] = None,
  path: Option[Path] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None,
  acl: Option[ACL] = None
) extends BaseMetadata

object ManagedFileMetadata {

  implicit val format: Format[ManagedFileMetadata] = (
    (__ \ OidKey.key).format[OrganisationId] and
    (__ \ PidKey.key).formatNullable[ProjectId] and
    (__ \ FidKey.key).formatNullable[FileId] and
    (__ \ UploadedByKey.key).formatNullable[UserId] and
    (__ \ VersionKey.key).format[Version] and
    (__ \ IsFolderKey.key).formatNullable[Boolean] and
    (__ \ PathKey.key).formatNullable[Path] and
    (__ \ DescriptionKey.key).formatNullable[String] and
    (__ \ LockKey.key).formatNullable[Lock] and
    (__ \ AclKey.key).formatNullable[ACL]
  )(ManagedFileMetadata.apply, unlift(ManagedFileMetadata.unapply))

}
