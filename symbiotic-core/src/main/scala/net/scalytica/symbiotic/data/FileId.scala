/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.data

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends Id

object FileId extends IdConverters[FileId] {
  lazy val empty: FileId = FileId("")

  override implicit def asId(s: String): FileId = FileId(s)
}
