/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

import scalacss.Defaults._

object FileTypes {

  sealed trait FileType

  case object Folder extends FileType

  case object FolderOpen extends FileType

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

  val fileTypes: Map[String, FileType] = Map(
    "application/pdf" -> PdfFile,
    "application/vnd.ms-excel" -> MSXlsFile,
    "application/vnd.ms-excel.addin.macroenabled.12" -> MSXlsFile,
    "application/vnd.ms-excel.sheet.binary.macroenabled.12" -> MSXlsFile,
    "application/vnd.ms-excel.sheet.macroenabled.12" -> MSXlsFile,
    "application/vnd.ms-excel.template.macroenabled.12" -> MSXlsFile,
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> MSXlsFile,
    "application/vnd.openxmlformats-officedocument.spreadsheetml.template" -> MSXlsFile,
    "application/msword" -> MSDocFile,
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> MSDocFile,
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template" -> MSDocFile,
    "application/vnd.ms-powerpoint" -> MSPptFile,
    "application/vnd.ms-powerpoint.addin.macroenabled.12" -> MSPptFile,
    "application/vnd.ms-powerpoint.presentation.macroenabled.12" -> MSPptFile,
    "application/vnd.ms-powerpoint.slide.macroenabled.12" -> MSPptFile,
    "application/vnd.ms-powerpoint.slideshow.macroenabled.12" -> MSPptFile,
    "application/vnd.ms-powerpoint.template.macroenabled.12" -> MSPptFile,
    "text/calendar" -> TxtFile,
    "text/css" -> TxtFile,
    "text/csv" -> TxtFile,
    "text/html" -> TxtFile,
    "text/plain" -> TxtFile,
    "text/richtext" -> TxtFile,
    "text/sgml" -> TxtFile,
    "text/tab-separated-values" -> TxtFile,
    "text/uri-list" -> TxtFile,
    "text/vnd.graphviz" -> TxtFile,
    "text/x-asm" -> TxtFile,
    "text/x-c" -> TxtFile,
    "text/x-fortran" -> TxtFile,
    "text/x-java-source,java" -> TxtFile,
    "text/x-pascal" -> TxtFile,
    "text/x-setext" -> TxtFile,
    "text/x-uuencode" -> TxtFile,
    "text/x-vcalendar" -> TxtFile,
    "text/x-vcard" -> TxtFile,
    "text/yaml" -> TxtFile,
    "audio/adpcm" -> SoundFile,
    "audio/basic" -> SoundFile,
    "audio/midi" -> SoundFile,
    "audio/mp4" -> SoundFile,
    "audio/mpeg" -> SoundFile,
    "audio/ogg" -> SoundFile,
    "audio/vnd.dts" -> SoundFile,
    "audio/vnd.dts.hd" -> SoundFile,
    "audio/x-aac" -> SoundFile,
    "audio/x-aiff" -> SoundFile,
    "audio/x-mpegurl" -> SoundFile,
    "audio/x-ms-wax" -> SoundFile,
    "audio/x-ms-wma" -> SoundFile,
    "audio/x-wav" -> SoundFile,
    "video/3gpp" -> MovieFile,
    "video/3gpp2" -> MovieFile,
    "video/h261" -> MovieFile,
    "video/h263" -> MovieFile,
    "video/h264" -> MovieFile,
    "video/jpeg" -> MovieFile,
    "video/mp4" -> MovieFile,
    "video/mpeg" -> MovieFile,
    "video/ogg" -> MovieFile,
    "video/quicktime" -> MovieFile,
    "video/x-f4v" -> MovieFile,
    "video/x-fli" -> MovieFile,
    "video/x-flv" -> MovieFile,
    "video/x-m4v" -> MovieFile,
    "video/x-ms-asf" -> MovieFile,
    "video/x-ms-wm" -> MovieFile,
    "video/x-ms-wmv" -> MovieFile,
    "video/x-ms-wmx" -> MovieFile,
    "video/x-ms-wvx" -> MovieFile,
    "video/x-msvideo" -> MovieFile,
    "image/bmp" -> ImageFile,
    "image/gif" -> ImageFile,
    "image/jpeg" -> ImageFile,
    "image/png" -> ImageFile,
    "image/svg+xml" -> ImageFile,
    "image/tiff" -> ImageFile,
    "image/webp" -> ImageFile,
    "image/x-cmu-raster" -> ImageFile,
    "image/x-cmx" -> ImageFile,
    "image/x-freehand" -> ImageFile,
    "image/x-icon" -> ImageFile,
    "image/x-pcx" -> ImageFile,
    "image/x-pict" -> ImageFile,
    "image/x-xbitmap" -> ImageFile,
    "image/x-xpixmap" -> ImageFile,
    "application/zip" -> ArchiveFile,
    "application/x-tar" -> ArchiveFile,
    "application/x-7z-compressed" -> ArchiveFile,
    "application/x-ace-compressed" -> ArchiveFile,
    "application/x-bzip" -> ArchiveFile,
    "application/x-bzip2" -> ArchiveFile,
    "application/x-stuffit" -> ArchiveFile,
    "application/x-stuffitx" -> ArchiveFile,
    "application/java-archive" -> ArchiveFile,
    "application/x-rar-compressed" -> ArchiveFile
  )

  def fromContentType(ctype: Option[String]): FileType =
    ctype.flatMap(ct => fileTypes.get(ct)).getOrElse(GenericFile)

  object Styles extends StyleSheet.Inline {

    import dsl._

    val contentTypes = Seq(
      SoundFile,
      MovieFile,
      ArchiveFile,
      PdfFile,
      TxtFile,
      MSDocFile,
      MSPptFile,
      MSXlsFile,
      ImageFile,
      GenericFile,
      FolderOpen,
      Folder
    )

    val ctDomain = Domain.ofValues[FileType](contentTypes: _*)

    val fontIcon = styleF(ctDomain) {
      case Folder => mixin(FontAwesome.folder)
      case FolderOpen => mixin(FontAwesome.folderOpen)
      case PdfFile => mixin(FontAwesome.pdf)
      case TxtFile => mixin(FontAwesome.txt)
      case ArchiveFile => mixin(FontAwesome.archive)
      case ImageFile => mixin(FontAwesome.image)
      case MovieFile => mixin(FontAwesome.movie)
      case SoundFile => mixin(FontAwesome.sound)
      case MSDocFile => mixin(FontAwesome.word)
      case MSXlsFile => mixin(FontAwesome.excel)
      case MSPptFile => mixin(FontAwesome.powerpoint)
      case _ => mixin(FontAwesome.file)
    }

    val icon = styleS(display.block)

    val IconLg = styleF(ctDomain)(ct => styleS(fontIcon(ct), icon, addClassName("fa-lg")))
    val Icon2x = styleF(ctDomain)(ct => styleS(fontIcon(ct), icon, addClassName("fa-2x")))
    val Icon3x = styleF(ctDomain)(ct => styleS(fontIcon(ct), icon, addClassName("fa-3x")))
    val Icon4x = styleF(ctDomain)(ct => styleS(fontIcon(ct), icon, addClassName("fa-4x")))
    val Icon5x = styleF(ctDomain)(ct => styleS(fontIcon(ct), icon, addClassName("fa-5x")))

  }

}
