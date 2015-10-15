/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.DateTimeConverters
import core.mongodb.DManFS
import models.docmanagement.MetadataKeys._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.iteratee.Enumerator

import scala.concurrent.ExecutionContext

/**
 * Represents a file to be up/down -loaded by a User.
 *
 * This is <i>NOT</i> a file in the sense of a java.util.File. But rather a wrapper around an InputStream with
 * quite a bit of extra Metadata information. The Metadata is mapped to the GridFS "<bucket>.files" collection, and
 * the InputStream is read from the "<bucket>.chunks" collection.
 */
case class File(
  id: Option[FileId] = None,
  filename: String,
  contentType: Option[String] = None,
  uploadDate: Option[DateTime] = None,
  length: Option[String] = None, // Same as the length field in GridFS...but as String to prevent data loss in JS clients
  stream: Option[FileStream] = None,
  // The following fields will be added to the GridFS Metadata in fs.files...
  metadata: FileMetadata) extends BaseFile {

  /**
   * Feeds the InputStream bytes into an Enumerator
   */
  def enumerate(implicit ec: ExecutionContext): Option[Enumerator[Array[Byte]]] = stream.map(s => Enumerator.fromStream(s))

}

object File extends DateTimeConverters with DManFS {

  val logger = LoggerFactory.getLogger(File.getClass)

  /**
   * Build up the necessary metadata for persisting in GridFS
   */
  def buildBSON(f: File): Metadata = {
    val builder = MongoDBObject.newBuilder
    f.id.foreach(builder += "_id" -> FileId.asObjId(_))
    FileMetadata.toBSON(f.metadata) ++ builder.result()
  }

  /**
   * Converter to map between a GridFSDBFile (from read operations) to a File
   *
   * @param gf GridFSDBFile
   * @return File
   */
  def fromGridFS(gf: GridFSDBFile): File = {
    val md = new MongoDBObject(gf.metaData)
    File(
      id = FileId.asMaybeId(gf._id),
      filename = gf.filename.getOrElse("no_name"),
      contentType = gf.contentType,
      uploadDate = Option(asDateTime(gf.uploadDate)),
      length = Option(gf.length.toString),
      stream = Option(gf.inputStream),
      metadata = FileMetadata.fromBSON(md)
    )
  }

  /**
   * Converter to map between a DBObject (from read operations) to a File.
   * This will typically be used when listing files in a GridFS <bucket>.files collection
   *
   * @param dbo DBObject
   * @return File
   */
  def fromBSON(dbo: DBObject): File = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject](MetadataKey)
    File(
      id = FileId.asMaybeId(mdbo._id),
      filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
      contentType = mdbo.getAs[String]("contentType"),
      uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
      length = mdbo.getAs[Long]("length").map(_.toString),
      stream = None,
      metadata = FileMetadata.fromBSON(md)
    )
  }
}