package net.scalytica.symbiotic.components.dman.foldercontent

import net.scalytica.symbiotic.logger._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderURIElem
import org.scalajs.dom
import org.scalajs.dom.raw.URL

import scala.concurrent.ExecutionContext.Implicits.global

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

  val $ : BackendScope[Props, Props]

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
    p.selectedFile.value
      .find(smf => smf.metadata.fid == mf.metadata.fid)
      .map { _ =>
        p.selectedFile.set(None)
      }
      .getOrElse {
        p.selectedFile.set(Option(mf))
      }
  }

  def downloadFile(e: ReactEvent, mf: ManagedFile): Callback = {
    import org.scalajs.dom.document
    e.preventDefault()
    Callback {
      ManagedFile.get(mf).map {
        case Right(blob) =>
          val url  = URL.createObjectURL(blob)
          val link = document.createElement("a")
          link.setAttribute("href", url)
          link.setAttribute("style", "display: none;")
          link.setAttribute("download", mf.filename)
          document.body.appendChild(link)
          link.domAsHtml.click()
          dom.window.setTimeout(Callback {
            URL.revokeObjectURL(url)
            document.body.removeChild(link)
          }.toJsFn, 100)
        case Left(err) =>
          log.warn(err)
      }
    }
  }

  /**
   * Renders the actual folder content.
   */
  def renderContent(p: Props) =
    p.files
      .filter(f => f.filename.toLowerCase.contains(p.filterText.toLowerCase))
      .map(
        mf =>
          if (mf.metadata.isFolder.get) renderFolder(p.selectedFile.value, mf)
          else
            renderFile(
              p.selectedFile.value,
              FileTypes.fromContentType(mf.contentType),
              mf
          )
      )

  /**
   * Renders a ManagedFile as a File
   */
  def renderFile(
      selected: Option[ManagedFile],
      contentType: FileTypes.FileType,
      wrapper: ManagedFile
  ): ReactElement

  /**
   * Renders a ManagedFile as a Folder
   */
  def renderFolder(
      selected: Option[ManagedFile],
      wrapper: ManagedFile
  ): ReactElement
}
