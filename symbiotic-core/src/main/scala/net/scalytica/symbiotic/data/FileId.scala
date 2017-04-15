package net.scalytica.symbiotic.data

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends Id

object FileId extends IdOps[FileId] {
  lazy val empty: FileId = FileId("")

  override implicit def asId(s: String): FileId = FileId(s)
}
