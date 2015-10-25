/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import models.docmanagement.MetadataKeys.IsFolderKey
import org.joda.time.DateTime

trait BaseFile {
  val id: Option[FileId]
  val filename: String
  val contentType: Option[String]
  val uploadDate: Option[DateTime]
  val metadata: FileMetadata
}

object BaseFile {

  def fromBSON(dbo: DBObject): BaseFile = {
    val isFolder = dbo.getAs[Boolean](IsFolderKey.full).getOrElse(false)
    if (isFolder) Folder.fromBSON(dbo)
    else File.fromBSON(dbo)
  }
}
