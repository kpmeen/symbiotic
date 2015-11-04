/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import core.security.authorisation.ACL
import models.base.{BaseMetadata, BaseMetadataConverter}
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

object ManagedFileMetadata extends BaseMetadataConverter[ManagedFileMetadata] {

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

  implicit override def toBSON(fmd: ManagedFileMetadata): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += OidKey.key -> fmd.oid.value
    builder += VersionKey.key -> fmd.version
    fmd.fid.foreach(builder += "fid" -> _.value)
    builder += IsFolderKey.key -> fmd.isFolder.getOrElse(false)
    fmd.uploadedBy.foreach(u => builder += UploadedByKey.key -> u.value)
    fmd.description.foreach(d => builder += DescriptionKey.key -> d)
    fmd.lock.foreach(l => builder += LockKey.key -> Lock.toBSON(l))
    fmd.path.foreach(f => builder += PathKey.key -> f.materialize)
    fmd.pid.foreach(p => builder += PidKey.key -> p.value)
    fmd.acl.foreach(a => builder += AclKey.key -> ACL.toBSON(a))

    builder.result()
  }

  implicit override def fromBSON(dbo: DBObject): ManagedFileMetadata = {
    ManagedFileMetadata(
      oid = dbo.as[String](OidKey.key),
      pid = dbo.getAs[String](PidKey.key),
      fid = dbo.getAs[String](FidKey.key),
      uploadedBy = dbo.getAs[String](UploadedByKey.key),
      version = dbo.getAs[Int](VersionKey.key).getOrElse(1),
      isFolder = dbo.getAs[Boolean](IsFolderKey.key),
      path = dbo.getAs[String](PathKey.key).map(Path.apply),
      description = dbo.getAs[String](DescriptionKey.key),
      lock = dbo.getAs[MongoDBObject](LockKey.key).map(Lock.fromBSON),
      acl = dbo.getAs[DBObject](AclKey.key).map(ACL.fromBSON)
    )
  }
}