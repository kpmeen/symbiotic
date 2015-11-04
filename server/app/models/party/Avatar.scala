/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.party

import java.io.InputStream

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.DateTimeConverters
import models.base.GridFSDocument
import models.party.PartyBaseTypes.UserId
import org.joda.time.DateTime

case class Avatar(
  id: Option[ObjectId] = None,
  uploadDate: Option[DateTime] = None,
  length: Option[String] = None,
  filename: String,
  contentType: Option[String] = None,
  stream: Option[InputStream] = None,
  metadata: AvatarMetadata
) extends GridFSDocument[AvatarMetadata]

object Avatar extends DateTimeConverters {

  def apply(uid: UserId, ctype: Option[String], s: Option[InputStream]): Avatar =
    Avatar(filename = uid.value, contentType = ctype, stream = s, metadata = AvatarMetadata(uid))

  /**
   * Converter to map between a GridFSDBFile (from read operations) to an Avatar image
   *
   * @param gf GridFSDBFile
   * @return Avatar
   */
  implicit def fromGridFS(gf: GridFSDBFile): Avatar = {
    val md = gf.metaData
    Avatar(
      id = gf._id,
      filename = gf.filename.getOrElse("no_name"),
      contentType = gf.contentType,
      uploadDate = Option(asDateTime(gf.uploadDate)),
      length = Option(gf.length.toString),
      stream = Option(gf.inputStream),
      metadata = md
    )
  }

  implicit def fromMaybeGridFS(mgf: Option[GridFSDBFile]): Option[Avatar] = mgf.map(fromGridFS)

  /**
   * Converter to map between a DBObject (from read operations) to a File.
   * This will typically be used when listing files in a GridFS <bucket>.files collection
   *
   * @param dbo DBObject
   * @return File
   */
  implicit def fromBSON(dbo: DBObject): Avatar = {
    val mdbo = new MongoDBObject(dbo)
    val md = mdbo.as[DBObject]("metadata")
    Avatar(
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