package net.scalytica.symbiotic.api.types

/**
 * Id for managed files (documents)
 */
case class FileId(value: String) extends Id

object FileId extends IdOps[FileId] {
  lazy val empty: FileId = FileId("")

  override implicit def asId(s: String): FileId = {
    if (s.length != 36)
      throw new IllegalArgumentException(s"$s is not a valid UUID.")

    FileId(s)
  }
}
