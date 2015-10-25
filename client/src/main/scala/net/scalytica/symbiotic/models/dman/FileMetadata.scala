/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

// TODO: Add support for ACL!!!
case class FileMetadata(
  oid: String,
  pid: Option[String] = None,
  uploadedBy: Option[String] = None,
  version: Int = 1,
  isFolder: Option[Boolean] = None,
  path: Option[String] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None)