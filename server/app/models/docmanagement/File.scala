/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.DateTimeConverters
import models.docmanagement.MetadataKeys._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Represents a file to be up/down -loaded by a User.
 *
 * This is <i>NOT</i> a file in the sense of a java.util.File. But rather a wrapper around an InputStream with
 * quite a bit of extra Metadata information. The Metadata is mapped to the GridFS "<bucket>.files" collection, and
 * the InputStream is read from the "<bucket>.chunks" collection.
 */
case class File(
  id: Option[ObjectId] = None,
  filename: String,
  contentType: Option[String] = None,
  uploadDate: Option[DateTime] = None,
  length: Option[String] = None,
  stream: Option[FileStream] = None,
  metadata: ManagedFileMetadata
) extends ManagedFile

object File extends DateTimeConverters {

  val logger = LoggerFactory.getLogger(File.getClass)

  /**
   * Converter to map between a GridFSDBFile (from read operations) to a File
   *
   * @param gf GridFSDBFile
   * @return File
   */
  implicit def fromGridFS(gf: GridFSDBFile): File = {
    File(
      id = gf._id,
      filename = gf.filename.getOrElse("no_name"),
      contentType = gf.contentType,
      uploadDate = Option(asDateTime(gf.uploadDate)),
      length = Option(gf.length.toString),
      stream = Option(gf.inputStream),
      metadata = gf.metaData
    )
  }

  implicit def fromMaybeGridFS(mgf: Option[GridFSDBFile]): Option[File] = mgf.map(fromGridFS)

  /**
   * Converter to map between a DBObject (from read operations) to a File.
   * This will typically be used when listing files in a GridFS <bucket>.files collection
   *
   * @param dbo DBObject
   * @return File
   */
  implicit def fromBSON(dbo: DBObject): File = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    File(
      id = mdbo._id,
      filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
      contentType = mdbo.getAs[String]("contentType"),
      uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
      length = mdbo.getAs[Long]("length").map(_.toString),
      stream = None,
      metadata = md
    )
  }
}