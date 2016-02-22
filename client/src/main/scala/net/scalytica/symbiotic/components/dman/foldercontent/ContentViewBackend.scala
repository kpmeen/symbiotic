/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

object ContentView_PS {

  case class Props(
    files: Seq[ManagedFile],
    selectedFolder: ExternalVar[Option[FileId]],
    selectedFile: ExternalVar[Option[ManagedFile]],
    filterText: String = "",
    ctl: RouterCtl[FolderURIElem]
  )

}

trait ContentViewBackend {

  import ContentView_PS._

  val $: BackendScope[Props, Props]

  /**
   * Navigate to a different folder...
   */
  def changeFolder(mf: ManagedFile): Callback =
    $.state.flatMap { s =>
      val fid = mf.fileId
      $.props.flatMap { p =>
        p.selectedFile.set(None) >>
        p.selectedFolder.set(Option(fid)) >>
        s.ctl.set(FolderURIElem(Option(fid.toUUID)))
      }
    }

  /**
   * Mark the given file as (de-)selected
   */
  def setSelected(mf: ManagedFile): Callback = $.props.flatMap { p =>
    println(mf.fileId)
    if (p.selectedFile.value.exists(smf => smf.metadata.fid == mf.metadata.fid)) p.selectedFile.set(None)
    else p.selectedFile.set(Option(mf))
  }

  /**
   * Renders the actual folder content.
   */
  def renderContent(p: Props) =
    p.files.filter(f => f.filename.toLowerCase.contains(p.filterText.toLowerCase)).map(mf =>
      if (mf.metadata.isFolder.get) renderFolder(p.selectedFile.value, mf)
      else renderFile(p.selectedFile.value, FileTypes.fromContentType(mf.contentType), mf)
    )

  /**
   * Renders a ManagedFile as a File
   */
  def renderFile(selected: Option[ManagedFile], contentType: FileTypes.FileType, wrapper: ManagedFile): ReactElement

  /**
   * Renders a ManagedFile as a Folder
   */
  def renderFolder(selected: Option[ManagedFile], wrapper: ManagedFile): ReactElement
}
