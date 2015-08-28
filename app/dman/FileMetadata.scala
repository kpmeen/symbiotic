/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import com.mongodb.casbah.Imports._
import dman.MetadataKeys._
import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class FileMetadata(
  cid: CustomerId,
  pid: Option[ProjectId] = None,
  uploadedBy: Option[UserId] = None,
  version: Version = 1,
  isFolder: Option[Boolean] = None,
  path: Option[Path] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None)

object FileMetadata {

  implicit val reads: Reads[FileMetadata] = (
    (__ \ CidKey.key).read[CustomerId] and
      (__ \ PidKey.key).readNullable[ProjectId] and
      (__ \ UploadedByKey.key).readNullable[UserId] and
      (__ \ VersionKey.key).read[Version] and
      (__ \ IsFolderKey.key).readNullable[Boolean] and
      (__ \ PathKey.key).readNullable[Path](Path.reads) and
      (__ \ DescriptionKey.key).readNullable[String] and
      (__ \ LockKey.key).readNullable[Lock]
    )(FileMetadata.apply _)

  implicit val writes: Writes[FileMetadata] = (
    (__ \ CidKey.key).write[CustomerId] and
      (__ \ PidKey.key).writeNullable[ProjectId] and
      (__ \ UploadedByKey.key).writeNullable[UserId] and
      (__ \ VersionKey.key).write[Version] and
      (__ \ IsFolderKey.key).writeNullable[Boolean] and
      (__ \ PathKey.key).writeNullable[Path](Path.writes) and
      (__ \ DescriptionKey.key).writeNullable[String] and
      (__ \ LockKey.key).writeNullable[Lock]
    )(unlift(FileMetadata.unapply))

  def toBSON(fmd: FileMetadata): MongoDBObject = {
    val builder = MongoDBObject.newBuilder

    builder += CidKey.key -> fmd.cid.value
    builder += VersionKey.key -> fmd.version
    builder += IsFolderKey.key -> fmd.isFolder.getOrElse(false)
    fmd.uploadedBy.foreach(u => builder += UploadedByKey.key -> u.value)
    fmd.description.foreach(d => builder += DescriptionKey.key -> d)
    fmd.lock.foreach(l => builder += LockKey.key -> Lock.toBSON(l))
    fmd.path.foreach(f => builder += PathKey.key -> f.materialize)
    fmd.pid.foreach(p => builder += PidKey.key -> p.value)

    builder.result()
  }

  def fromBSON(dbo: DBObject): FileMetadata = {
    FileMetadata(
      cid = dbo.as[String](CidKey.key),
      pid = dbo.getAs[String](PidKey.key),
      uploadedBy = dbo.getAs[String](UploadedByKey.key),
      version = dbo.getAs[Int](VersionKey.key).getOrElse(1),
      isFolder = dbo.getAs[Boolean](IsFolderKey.key),
      path = dbo.getAs[String](PathKey.key).map(Path.apply),
      description = dbo.getAs[String](DescriptionKey.key),
      lock = dbo.getAs[MongoDBObject](LockKey.key).map(Lock.fromBSON)
    )
  }

}