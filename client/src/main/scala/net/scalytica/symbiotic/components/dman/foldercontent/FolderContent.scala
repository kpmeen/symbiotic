/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.extra.{ExternalVar, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.components.dman.PathCrumb
import net.scalytica.symbiotic.components.{FilterInput, IconButton, Spinner}
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderPath
import org.scalajs.dom.raw.HTMLFormElement
import org.scalajs.jquery._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalacss.ScalaCssReact._

object FolderContent {

  sealed trait ViewType

  case object IconViewType extends ViewType

  case object TableViewType extends ViewType

  case object ColumnViewType extends ViewType

  case class Props(
    oid: String,
    folder: Option[String],
    files: Seq[ManagedFile],
    ctl: RouterCtl[FolderPath],
    status: AjaxStatus,
    selected: ExternalVar[Option[ManagedFile]],
    filterText: String = "",
    viewType: ViewType = IconViewType
  )

  class Backend(val $: BackendScope[Props, Props]) {

    /**
     * Triggers the default HTML5 file upload dialogue
     */
    def showFileUploadDialogue(e: ReactEventI): Callback = Callback(jQuery("#ManagedFileUplad").click())

    /**
     * Default content loading...
     */
    def loadContent(): Callback = $.props.map(p => loadContent(p))

    /**
     * Load content based on props...
     */
    def loadContent(p: Props) =
      ManagedFile.load(p.oid, p.folder).map {
        case Right(res) => $.modState(_.copy(folder = p.folder, files = res, status = Finished))
        case Left(failed) => $.modState(_.copy(folder = p.folder, files = Nil, status = failed))
      }.recover {
        case err =>
          log.error(err)
          $.modState(_.copy(folder = p.folder, files = Nil, status = Failed(err.getMessage)))
      }.map(_.runNow())

    /**
     * Uploads a managed file...
     */
    def uploadFile(e: ReactEventI): Callback =
      $.props.map { props =>
        val form = e.currentTarget.parentElement.asInstanceOf[HTMLFormElement]
        ManagedFile.upload(props.oid, props.folder.getOrElse("/"), form)(Callback(loadContent().runNow()))
      }

    /**
     * Triggers a file download
     */
    def downloadFile(e: ReactEventI): Callback =
    // I'm just lazy...fuck it...this will trigger a log message in the web console
      $.props.map(p => p.selected.value.foreach(f => jQuery(s"#${f.id}")(0).click()))

    def createFolder(e: ReactEventI) = {
      Callback(log.debug("Clicked the add folder button"))
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
        p.selected.value.map { mf =>
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
            p.selected.set(mfm) >>
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
                ^.id := "ManagedFileUplad",
                ^.name := "file",
                ^.`type` := "file",
                ^.visibility.hidden,
                ^.onChange ==> uploadFile
              )
            ),
            PathCrumb(p.oid, p.folder.getOrElse("/"), p.selected, p.ctl),
            // TODO: Wrap in a btn-toolbar
            <.div(^.className := "btn-toolbar",
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton("fa fa-folder", createFolder)
              ),
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton("fa fa-upload", showFileUploadDialogue),
                IconButton("fa fa-download", downloadFile),
                p.selected.value.map { mf =>
                  val lockCls = mf.metadata.lock.fold("fa fa-lock")(l => "fa fa-unlock")
                  IconButton(lockCls, changeLock)
                }
              ),
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton("fa fa-th-large", showIconView),
                IconButton("fa fa-table", showTableView),
                IconButton("fa fa-columns", showColumnView)
              ),
              <.div(^.className := "btn-group", ^.role := "group",
                FilterInput(
                  id = s"searchBox-${p.folder.getOrElse("NA").replaceAll("/", "_")}",
                  label = "Filter content",
                  onTextChange = onTextChange
                )
              )
            ),
            s.viewType match {
              case IconViewType =>
                IconView(p.oid, s.files, p.selected, s.filterText, s.ctl)
              case TableViewType =>
                TableView(p.oid, s.files, p.selected, s.filterText, s.ctl)
              case ColumnViewType =>
                <.span("not implmented")
            }
          )
        case Failed(err) => <.div(^.className := "container-fluid", err)
      }
    }
  }

  implicit val fwReuse = Reusability.fn((p: Props, s: Props) =>
    p.folder == s.folder &&
      p.status == s.status &&
      p.filterText == s.filterText &&
      p.selected.value == s.selected.value &&
      p.files.size == s.files.size &&
      p.viewType == s.viewType
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .componentDidMount($ => Callback.ifTrue($.isMounted(), $.backend.loadContent()))
    .componentWillReceiveProps(cwrp =>
      Callback.ifTrue(
        cwrp.$.isMounted() && cwrp.nextProps.selected.value.isEmpty,
        Callback(cwrp.$.backend.loadContent(cwrp.nextProps))
      )
    )
    .build

  def apply(
    oid: String,
    folder: Option[String],
    files: Seq[ManagedFile],
    selected: ExternalVar[Option[ManagedFile]],
    ctl: RouterCtl[FolderPath]
  ) = component(Props(oid, folder, files, ctl, Loading, selected))
}
