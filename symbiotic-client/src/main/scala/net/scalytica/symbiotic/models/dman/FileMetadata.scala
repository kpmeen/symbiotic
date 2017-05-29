/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

case class FileMetadata(
    owner: Option[String],
    // All files **must** have a FileId
    fid: String,
    uploadedBy: Option[String] = None,
    version: Int = 1,
    isFolder: Option[Boolean] = None,
    path: Option[String] = None,
    description: Option[String] = None,
    lock: Option[Lock] = None
)
