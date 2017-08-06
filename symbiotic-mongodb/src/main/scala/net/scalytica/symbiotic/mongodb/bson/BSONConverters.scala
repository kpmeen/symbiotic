package net.scalytica.symbiotic.mongodb.bson

import java.util.UUID

import akka.stream.scaladsl.StreamConverters
import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.gridfs.GridFSDBFile
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.ResourceOwner._
import net.scalytica.symbiotic.api.types.{ManagedFile, _}
import net.scalytica.symbiotic.mongodb.bson.BaseBSONConverters.DateTimeBSONConverter // scalastyle:ignore
import org.joda.time.DateTime

object BSONConverters {

  object Implicits extends FileFolderBSONConverter with DateTimeBSONConverter

  trait LockBSONConverter extends DateTimeBSONConverter {
    implicit def lock_toBSON(lock: Lock): MongoDBObject = {
      MongoDBObject(
        "by"   -> lock.by.value,
        "date" -> lock.date.toDate
      )
    }

    implicit def lock_fromBSON(
        dbo: MongoDBObject
    )(implicit ctx: SymbioticContext): Lock = {
      Lock(
        by = ctx.toUserId(dbo.as[String]("by")),
        date = dbo.as[java.util.Date]("date")
      )
    }
  }

  trait ManagedMetadataBSONConverter extends LockBSONConverter {

    implicit def extraAttribs_toBSON(mm: MetadataMap): DBObject = {
      mm.plainMap.map {
        case (key, v: DateTime) => key -> v.toDate
        case kv                 => kv
      }.asDBObject
    }

    implicit def extraAttribs_fromBSON(dbo: MongoDBObject): MetadataMap = {
      // implicit conversion to a MetadataMap
      dbo.toMap[String, Any]
    }

    implicit def optExtraAttribs_fromBSON(
        maybeDbo: Option[MongoDBObject]
    ): Option[MetadataMap] = maybeDbo.map(extraAttribs_fromBSON)

    implicit def owner_toBSON(o: Owner): DBObject = {
      MongoDBObject(
        OwnerIdKey.key   -> o.id.value,
        OwnerTypeKey.key -> o.ownerType.tpe
      )
    }

    implicit def owner_fromBSON(
        dbo: MongoDBObject
    )(implicit ctx: SymbioticContext): Owner = {
      val tpe: OwnerType = dbo.as[String](OwnerTypeKey.key)
      val idStr          = dbo.as[String](OwnerIdKey.key)
      val id = tpe match {
        case UserOwner => ctx.toUserId(idStr)
        case OrgOwner  => ctx.toOrgId(idStr)
      }
      Owner(id, tpe)
    }

    implicit def managedmd_toBSON(fmd: ManagedMetadata): DBObject = {
      val b = MongoDBObject.newBuilder
      fmd.owner.foreach(o => b += OwnerKey.key -> owner_toBSON(o))
      b += VersionKey.key -> fmd.version
      fmd.fid.foreach(b += "fid" -> _.value)
      b += IsFolderKey.key -> fmd.isFolder.getOrElse(false)
      fmd.uploadedBy.foreach(u => b += UploadedByKey.key   -> u.value)
      fmd.description.foreach(d => b += DescriptionKey.key -> d)
      fmd.lock.foreach(l => b += LockKey.key               -> lock_toBSON(l))
      fmd.path.foreach(f => b += PathKey.key               -> f.materialize)
      fmd.extraAttributes.foreach(
        mm => b += ExtraAttributesKey.key -> extraAttribs_toBSON(mm)
      )

      b.result()
    }

    implicit def managedmd_fromBSON(
        dbo: DBObject
    )(implicit ctx: SymbioticContext): ManagedMetadata = {
      ManagedMetadata(
        owner = dbo.getAs[MongoDBObject](OwnerKey.key).map(owner_fromBSON),
        fid = dbo.getAs[String](FidKey.key),
        uploadedBy = dbo.getAs[String](UploadedByKey.key).map(ctx.toUserId),
        version = dbo.getAs[Int](VersionKey.key).getOrElse(1),
        isFolder = dbo.getAs[Boolean](IsFolderKey.key),
        path = dbo.getAs[String](PathKey.key).map(Path.apply),
        description = dbo.getAs[String](DescriptionKey.key),
        lock = dbo.getAs[MongoDBObject](LockKey.key).map(lock_fromBSON),
        extraAttributes = dbo.getAs[MongoDBObject](ExtraAttributesKey.key)
      )
    }
  }

  trait FileFolderBSONConverter
      extends ManagedMetadataBSONConverter
      with DateTimeBSONConverter {

    implicit def folder_fromBSON(
        dbo: DBObject
    )(implicit ctx: SymbioticContext): Folder = {
      val mdbo = new MongoDBObject(dbo)
      val md   = mdbo.as[DBObject](MetadataKey)
      Folder(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.as[String]("filename"),
        fileType = mdbo.getAs[String]("contentType"),
        metadata = managedmd_fromBSON(md)
      )
    }

    /**
     * Converter to map between a GridFSDBFile (from read operations) to a File
     *
     * @param gf GridFSDBFile
     * @return File
     */
    implicit def file_fromGridFS(
        gf: GridFSDBFile
    )(implicit ctx: SymbioticContext): File = {
      File(
        id = gf.getAs[String]("_id").map(UUID.fromString),
        filename = gf.filename.getOrElse("no_name"),
        fileType = gf.contentType,
        uploadDate = Option(asDateTime(gf.uploadDate)),
        length = Option(gf.length.toString),
        stream = Option(StreamConverters.fromInputStream(() => gf.inputStream)),
        metadata = managedmd_fromBSON(gf.metaData)
      )
    }

    implicit def files_fromGridFS(
        gfs: Seq[GridFSDBFile]
    )(implicit ctx: SymbioticContext): Seq[File] = gfs.map(file_fromGridFS)

    implicit def file_fromMaybeGridFS(
        mgf: Option[GridFSDBFile]
    )(implicit ctx: SymbioticContext): Option[File] = mgf.map(file_fromGridFS)

    /**
     * Converter to map between a DBObject (from read operations) to a File.
     * This will typically be used when listing files in a GridFS <bucket>.files
     * collection
     *
     * @param dbo DBObject
     * @return File
     */
    implicit def file_fromBSON(
        dbo: DBObject
    )(implicit ctx: SymbioticContext): File = {
      val mdbo = new MongoDBObject(dbo)
      val md   = mdbo.as[DBObject](MetadataKey)

      File(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
        fileType = mdbo.getAs[String]("contentType"),
        uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
        length = mdbo.getAs[Long]("length").map(_.toString),
        stream = None,
        metadata = managedmd_fromBSON(md)
      )
    }

    implicit def files_fromBSON(
        dbos: Seq[DBObject]
    )(implicit ctx: SymbioticContext): Seq[File] = dbos.map(file_fromBSON)

    implicit def managedfile_fromBSON(
        dbo: DBObject
    )(implicit ctx: SymbioticContext): ManagedFile = {
      val isFolder = dbo.getAs[Boolean](IsFolderKey.full).getOrElse(false)
      if (isFolder) folder_fromBSON(dbo)
      else file_fromBSON(dbo)
    }

    implicit def managedfiles_fromBSON(
        dbos: Seq[DBObject]
    )(implicit ctx: SymbioticContext): Seq[ManagedFile] =
      dbos.map(managedfile_fromBSON)
  }

}
