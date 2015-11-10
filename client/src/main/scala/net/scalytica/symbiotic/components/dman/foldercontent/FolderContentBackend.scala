/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderPath



object FolderContent_PS {

  case class Props(
    oid: String,
    files: Seq[ManagedFile],
    selected: ExternalVar[Option[ManagedFile]],
    filterText: String = "",
    ctl: RouterCtl[FolderPath]
  )
}

trait FolderContentBackend {

  import FolderContent_PS._

  val $: BackendScope[Props, Unit]

  /**
   * Navigate to a different folder...
   */
  def changeFolder(mf: ManagedFile): Callback =
    $.props.flatMap { p =>
      p.ctl.set(FolderPath(UUID.fromString(p.oid), mf.path))  >> p.selected.set(None)
    }

  /**
   * Mark the given file as (de-)selected
   */
  def setSelected(mf: ManagedFile): Callback = $.props.flatMap { p =>
    if (p.selected.value.exists(smf => smf.metadata.fid == mf.metadata.fid)) p.selected.set(None)
    else p.selected.set(Option(mf))
  }

  /**
   * Renders the actual folder content.
   */
  def renderContent(p: Props) =
    p.files.filter(f => f.filename.toLowerCase.contains(p.filterText.toLowerCase)).map(mf =>
      if (mf.metadata.isFolder.get) renderFolder(p.selected.value, mf)
      else renderFile(p.selected.value, FileTypes.fromContentType(mf.contentType), mf)
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
