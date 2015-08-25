/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import org.joda.time.DateTime

trait FileBase[A] {

  val id: Option[FileId]
  val filename: String
  val uploadDate: Option[DateTime]

  // The following fields will be added to the GridFS Metadata in fs.files...
  val cid: CustomerId
  val pid: Option[ProjectId]
  val uploadedBy: Option[UserId]
  val isFolder: Option[Boolean]
  val folder: Option[Folder]
  val description: Option[String]

}
