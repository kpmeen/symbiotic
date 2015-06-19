/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import scala.scalajs.js.Date

case class FileWrapper(
  id: String,
  filename: String,
  contentType: Option[String]= None,
  uploadDate: Option[Date] = None,
  size: Option[Long] = None, // Same as the length field in GridFS
  // The following fields will be added to the GridFS Metadata in fs.files...
  cid: String,
  pid: Option[String] = None,
  uploadedBy: Option[String] = None,
  version: Int = 1,
  isFolder: Option[Boolean] = None,
  folder: Option[String] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None)

case class Lock(by: String, date: Date)