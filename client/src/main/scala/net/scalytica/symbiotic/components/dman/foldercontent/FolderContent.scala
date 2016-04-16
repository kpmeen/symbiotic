/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.extra.{LogLifecycle, ExternalVar, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.components.dman.{FileInfo, PathCrumb}
import net.scalytica.symbiotic.components.{FilterInput, IconButton, Modal, Spinner}
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderURIElem
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLFormElement, HTMLInputElement}
import org.scalajs.jquery.jQuery
import net.scalytica.symbiotic.core.facades.Bootstrap._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object FolderContent {

  sealed trait ViewType

  case object IconViewType extends ViewType

  case object TableViewType extends ViewType

  case object ColumnViewType extends ViewType

  val ManagedFileUploadId = "ManagedFileUpload"
  val FolderNameInputId = "FolderNameInput"
  val AddFolderModalId = "AddFolderModal"

  case class Props(
    selectedFolder: ExternalVar[Option[FileId]],
    currPath: Option[String],
    ftree: ExternalVar[FTree],
    reloadFTree: Callback,
    files: Seq[ManagedFile],
    ctl: RouterCtl[FolderURIElem],
    status: AjaxStatus,
    selectedFile: ExternalVar[Option[ManagedFile]],
    filterText: String = "",
    viewType: ViewType = TableViewType
  )

  class Backend(val $: BackendScope[Props, Props]) {

    /**
     * Triggers the default HTML5 file upload dialogue
     */
    def showFileUploadDialogue(e: ReactEventI): Callback = Callback {
      dom.document.getElementById(ManagedFileUploadId).domAsHtml.click()
    }

    /**
     * Default content loading...
     */
    def loadContent(): Callback = $.props.map(p => loadContent(p)).void

    /**
     * Load content based on props...
     */
    def loadContent(p: Props) =
      p.selectedFolder.value.map(fid => ManagedFile.load(fid)).getOrElse(ManagedFile.load(None)).map {
        case Right(res) =>
          $.modState(_.copy(
            currPath = res.folder.flatMap(_.path),
            files = res.content,
            status = Finished
          ))
        case Left(failed) =>
          $.modState(_.copy(files = Nil, status = failed))
      }.recover {
        case err =>
          log.error(err)
          $.modState(_.copy(files = Nil, status = Failed(err.getMessage)))
      }.map(_.runNow())

    /**
     * Uploads a managed file...
     */
    def uploadFile(e: ReactEventI): Callback =
      $.props.map { props =>
        val form = e.currentTarget.parentElement.asInstanceOf[HTMLFormElement]
        ManagedFile.upload(props.selectedFolder.value, form)(Callback(loadContent().runNow()))
      }

    /**
     * Triggers a file download
     */
    def downloadFile(e: ReactEventI): Callback =
      $.props.map(p => p.selectedFile.value.foreach(f =>
        dom.document.getElementById(f.fileId.value).domAsHtml.click()
      ))

    def createFolder[A <: ReactUIEvent](evt: A): Callback = {
      val fnameInput = Option(dom.document.getElementById(FolderNameInputId).asInstanceOf[HTMLInputElement])
      fnameInput.filterNot(e => e.value == "").map { in =>
        $.props.map { p =>
          Callback.future {
            ManagedFile.addFolder(p.selectedFolder.value, in.value).map {
              case Finished => $.state.flatMap(_.reloadFTree) >> loadContent()
              case Failed(err) => Callback.log(err)
              case _ => Callback.log("Should not happen!")

            }.andThen {
              case _ =>
                in.value = ""
                jQuery(s"#$AddFolderModalId").modal("hide")
            }
          }.runNow()
        }
      }.getOrElse(Callback.empty)
    }

    /**
     * Show the content as an Icon view
     */
    def showIconView(e: ReactEventI): Callback = $.modState(s => s.copy(viewType = IconViewType))

    /**
     * Show the content as a Table view
     */
    def showTableView(e: ReactEventI): Callback = $.modState(s => s.copy(viewType = TableViewType))

    /**
     * Show the content as a Column view
     */
    def showColumnView(e: ReactEventI): Callback = $.modState(s => s.copy(viewType = ColumnViewType))

    /**
     * Handle text change in the search/filter component
     */
    def onTextChange(text: String): Callback = $.modState(_.copy(filterText = text))

    def changeLock(e: ReactEventI): Callback =
      $.props.flatMap(p =>
        p.selectedFile.value.map { mf =>
          val fid = mf.metadata.fid
          val fmf: Future[Option[ManagedFile]] =
            if (mf.metadata.lock.isDefined) ManagedFile.unlock(fid).map {
              case Finished =>
                Some(mf.copy(metadata = mf.metadata.copy(lock = None)))
              case Failed(err) =>
                None
              case _ =>
                None
            }
            else ManagedFile.lock(fid).map {
              case Right(lock) =>
                Some(mf.copy(metadata = mf.metadata.copy(lock = Option(lock))))
              case Left(err) =>
                None
            }

          Callback.future(fmf.map { mfm =>
            p.selectedFile.set(mfm) >>
              $.modState(_.copy(status = Loading)) >>
              Callback(loadContent(p))
          })
        }.getOrElse(Callback.empty)
      )

    /**
     * Render the actual component
     */
    def render(p: Props, s: Props) = {
      s.status match {
        case Loading =>
          <.div(^.className := "container-fluid",
            <.div(^.className := "panel panel-default",
              <.div(^.className := "panel-body",
                <.div(FolderContentStyle.loading, Spinner(Medium))
              )
            )
          )
        case Finished =>
          <.div(^.className := "container-fluid",
            <.form(^.encType := "multipart/form-data",
              <.input(
                ^.id := ManagedFileUploadId,
                ^.name := "file",
                ^.`type` := "file",
                ^.visibility.hidden,
                ^.onChange ==> uploadFile
              )
            ),
            {
              val pathElems = p.selectedFolder.value.map(fid => p.ftree.value.root.buildPathLink(fid))
              PathCrumb(p.selectedFolder, pathElems.getOrElse(Seq.empty), p.selectedFile, p.ctl)
            },
            <.div(^.className := "btn-toolbar",
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton(
                  "fa fa-folder",
                  Seq(
                    "data-toggle".reactAttr := "modal",
                    "data-target".reactAttr := s"#$AddFolderModalId"
                  )
                )
              ),
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton("fa fa-upload", Seq.empty, showFileUploadDialogue),
                IconButton("fa fa-download", Seq.empty, downloadFile),
                p.selectedFile.value.map { mf =>
                  val lockCls = mf.metadata.lock.fold("fa fa-lock")(l => "fa fa-unlock")
                  IconButton(lockCls, Seq.empty, changeLock)
                }
              ),
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton("fa fa-th-large", Seq.empty, showIconView),
                IconButton("fa fa-table", Seq.empty, showTableView),
                IconButton("fa fa-columns", Seq.empty, showColumnView)
              ),
              <.div(^.className := "btn-group", ^.role := "group",
                FilterInput(
                  id = s"searchBox-${p.selectedFolder.value.map(_.value).getOrElse("NA")}",
                  label = "Filter content",
                  onTextChange = onTextChange
                )
              )
            ),
            s.viewType match {
              case IconViewType =>
                IconView(s.files, p.selectedFolder, p.selectedFile, s.filterText, s.ctl)
              case TableViewType =>
                TableView(s.files, p.selectedFolder, p.selectedFile, s.filterText, s.ctl)
              case ColumnViewType =>
                <.span("not implmented")
            },

            Modal(
              id = AddFolderModalId,
              header = Some("Add folder..."),
              body = {
                <.div(
                  ^.className := "form-group",
                  ^.onKeyPress ==> { (e: ReactKeyboardEvent) => if (e.key == "Enter") createFolder(e) else Callback.empty },
                  <.label(^.`for` := FolderNameInputId, "Folder name"),
                  <.input(^.id := FolderNameInputId, ^.`type` := "text", ^.className := "form-control")
                )
              },
              footer = Some(
                <.div(
                  <.button(^.`type` := "button", ^.className := "btn btn-default", "data-dismiss".reactAttr := "modal", "Cancel"),
                  <.button(
                    ^.`type` := "button",
                    ^.className := "btn btn-default",
                    ^.onClick ==> createFolder
                  )("Add")
                )
              )
            )
          )
        case Failed(err) =>
          <.div(^.className := "container-fluid", err)
      }
    }
  }

  implicit val fwReuse = Reusability.fn((p: Props, s: Props) =>
    p.selectedFolder == s.selectedFolder &&
      p.currPath == s.currPath &&
      p.status == s.status &&
      p.filterText == s.filterText &&
      p.selectedFile.value == s.selectedFile.value &&
      p.files.size == s.files.size &&
      p.viewType == s.viewType
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
//    .configure(LogLifecycle.short)
    .componentDidMount($ => Callback.when($.isMounted())($.backend.loadContent()))
    .componentWillReceiveProps(cwrp =>
      Callback.when(cwrp.$.isMounted() && cwrp.nextProps.selectedFile.value.isEmpty)(
        Callback(cwrp.$.backend.loadContent(cwrp.nextProps))
      )
    )
    .build

  def apply(
    selectedFolder: ExternalVar[Option[FileId]],
    ftree: ExternalVar[FTree],
    reloadFTree: Callback,
    files: Seq[ManagedFile],
    selectedFile: ExternalVar[Option[ManagedFile]],
    ctl: RouterCtl[FolderURIElem]
  ) = component(Props(selectedFolder, None, ftree, reloadFTree, files, ctl, Loading, selectedFile))
}
