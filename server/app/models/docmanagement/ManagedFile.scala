/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import com.mongodb.casbah.Imports._
import models.base.GridFSDocument
import models.docmanagement.MetadataKeys._

trait ManagedFile extends GridFSDocument[ManagedFileMetadata]

object ManagedFile {
  def fromBSON(dbo: DBObject): ManagedFile = {
    val isFolder = dbo.getAs[Boolean](IsFolderKey.full).getOrElse(false)
    if (isFolder) Folder.fromBSON(dbo)
    else File.fromBSON(dbo)
  }
}
