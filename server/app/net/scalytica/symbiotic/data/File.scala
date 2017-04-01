/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.data

import java.util.UUID

import org.joda.time.DateTime

/**
 * Represents a file to be up/down -loaded by a User.
 *
 * This is <i>NOT</i> a file in the sense of a java.util.File. But rather a
 * wrapper around an InputStream with quite a bit of extra Metadata information.
 * The Metadata is mapped to the GridFS "<bucket>.files" collection, and the
 * InputStream is read from the "<bucket>.chunks" collection.
 */
case class File(
    id: Option[UUID] = None,
    filename: String,
    contentType: Option[String] = None,
    uploadDate: Option[DateTime] = None,
    length: Option[String] = None,
    stream: Option[FileStream] = None,
    metadata: ManagedFileMetadata
) extends ManagedFile

object File extends ManagedFileExtensions[File] {

  override def mapTo(mf: ManagedFile): Option[File] =
    mf.metadata.isFolder.flatMap {
      case true => None
      case false =>
        Option(
          File(
            id = mf.id,
            filename = mf.filename,
            contentType = mf.contentType,
            uploadDate = mf.uploadDate,
            length = mf.length,
            stream = mf.stream,
            metadata = mf.metadata
          )
        )
    }

}
