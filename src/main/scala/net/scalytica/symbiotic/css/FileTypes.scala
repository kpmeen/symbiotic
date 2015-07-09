/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

object FileTypes {

  sealed trait FileType

  case object Folder extends FileType

  case object GenericFile extends FileType

  case object PdfFile extends FileType

  case object MSXlsFile extends FileType

  case object MSDocFile extends FileType

  case object MSPptFile extends FileType

  case object TxtFile extends FileType

  case object SoundFile extends FileType

  case object MovieFile extends FileType

  case object ImageFile extends FileType

  case object ArchiveFile extends FileType

  object ContentTypes {
    val pdf = "application/pdf"
    val txt = "text/plain"
    val html = "text/html"
    val xml = "application/xml"
    val word = "application/msword"
  }

  def fromContentType(str: Option[String]): FileType =
    str.map {
      case ContentTypes.pdf => PdfFile
      case ContentTypes.txt => TxtFile
      case _ => GenericFile
    }.getOrElse(GenericFile)

}
