package net.scalytica.symbiotic.mongodb.bson

import java.util.UUID

import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.gridfs.GridFSDBFile
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data.MetadataKeys._
import net.scalytica.symbiotic.data._
import net.scalytica.symbiotic.mongodb.bson.BaseBSONConverters.DateTimeBSONConverter

object BSONConverters {

  object Implicits extends FileFolderBSONConverter

  trait LockBSONConverter extends DateTimeBSONConverter {
    implicit def lock_toBSON(lock: Lock): MongoDBObject = {
      MongoDBObject(
        "by"   -> lock.by.value,
        "date" -> lock.date.toDate
      )
    }

    implicit def lock_fromBSON(dbo: MongoDBObject): Lock = {
      Lock(
        by = UserId.asId(dbo.as[String]("by")),
        date = dbo.as[java.util.Date]("date")
      )
    }
  }

  trait ManagedFileMetadataBSONConverter extends LockBSONConverter {

    implicit def managedfmd_toBSON(fmd: ManagedFileMetadata): DBObject = {
      val b = MongoDBObject.newBuilder
      fmd.owner.foreach(o => b += OwnerKey.key -> o.value)
      b += VersionKey.key -> fmd.version
      fmd.fid.foreach(b += "fid" -> _.value)
      b += IsFolderKey.key -> fmd.isFolder.getOrElse(false)
      fmd.uploadedBy.foreach(u => b += UploadedByKey.key   -> u.value)
      fmd.description.foreach(d => b += DescriptionKey.key -> d)
      fmd.lock.foreach(l => b += LockKey.key               -> lock_toBSON(l))
      fmd.path.foreach(f => b += PathKey.key               -> f.materialize)

      b.result()
    }

    implicit def managedfmd_fromBSON(dbo: DBObject): ManagedFileMetadata = {
      ManagedFileMetadata(
        owner = dbo.getAs[String](OwnerKey.key).map(UserId.apply),
        fid = dbo.getAs[String](FidKey.key),
        uploadedBy = dbo.getAs[String](UploadedByKey.key),
        version = dbo.getAs[Int](VersionKey.key).getOrElse(1),
        isFolder = dbo.getAs[Boolean](IsFolderKey.key),
        path = dbo.getAs[String](PathKey.key).map(Path.apply),
        description = dbo.getAs[String](DescriptionKey.key),
        lock = dbo.getAs[MongoDBObject](LockKey.key).map(lock_fromBSON)
      )
    }
  }

  trait FileFolderBSONConverter
      extends ManagedFileMetadataBSONConverter
      with DateTimeBSONConverter {

    implicit def folder_fromBSON(dbo: DBObject): Folder = {
      val mdbo = new MongoDBObject(dbo)
      val md   = mdbo.as[DBObject](MetadataKey)
      Folder(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.as[String]("filename"),
        metadata = managedfmd_fromBSON(md)
      )
    }

    /**
     * Converter to map between a GridFSDBFile (from read operations) to a File
     *
     * @param gf GridFSDBFile
     * @return File
     */
    implicit def file_fromGridFS(gf: GridFSDBFile): File = {
      File(
        id = gf.getAs[String]("_id").map(UUID.fromString),
        filename = gf.filename.getOrElse("no_name"),
        contentType = gf.contentType,
        uploadDate = Option(asDateTime(gf.uploadDate)),
        length = Option(gf.length.toString),
        stream = Option(gf.inputStream),
        metadata = managedfmd_fromBSON(gf.metaData)
      )
    }

    implicit def file_fromMaybeGridFS(
        mgf: Option[GridFSDBFile]
    ): Option[File] =
      mgf.map(file_fromGridFS)

    /**
     * Converter to map between a DBObject (from read operations) to a File.
     * This will typically be used when listing files in a GridFS <bucket>.files
     * collection
     *
     * @param dbo DBObject
     * @return File
     */
    implicit def file_fromBSON(dbo: DBObject): File = {
      val mdbo = new MongoDBObject(dbo)
      val md   = mdbo.as[DBObject](MetadataKey)
      File(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
        contentType = mdbo.getAs[String]("contentType"),
        uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
        length = mdbo.getAs[Long]("length").map(_.toString),
        stream = None,
        metadata = managedfmd_fromBSON(md)
      )
    }

    implicit def managedfile_fromBSON(dbo: DBObject): ManagedFile = {
      val isFolder = dbo.getAs[Boolean](IsFolderKey.full).getOrElse(false)
      if (isFolder) folder_fromBSON(dbo)
      else file_fromBSON(dbo)
    }
  }

}
