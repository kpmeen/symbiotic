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
import net.scalytica.symbiotic.components.{IconButton, SearchBox, Spinner}
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderPath
import org.scalajs.dom.raw.HTMLFormElement
import org.scalajs.jquery._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scalacss.ScalaCssReact._

object FolderContent {

  case class Props(
    oid: String,
    folder: Option[String],
    files: Seq[ManagedFile],
    ctl: RouterCtl[FolderPath],
    status: AjaxStatus,
    selected: ExternalVar[Option[ManagedFile]],
    showFilter: Boolean = false,
    filterText: String = ""
  )

  type State = Props

  class Backend(val $: BackendScope[Props, State]) {

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

    /**
     * Toggles the search box between hidden and visible
     */
    def showSearchBox(e: ReactEventI): Callback = $.modState(curr => curr.copy(showFilter = !curr.showFilter))

    def createFolder(e: ReactEventI) = {
      Callback(log.debug("Clicked the add folder button"))
    }

    /**
     * Handle text change in the search/filter component
     */
    def onTextChange(text: String): Callback = $.modState(_.copy(filterText = text))

    /**
     * Render the actual component
     */
    def render(p: Props, s: State) = {
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
                IconButton("fa fa-folder", createFolder),
                IconButton("fa fa-upload", showFileUploadDialogue),
                IconButton("fa fa-download", downloadFile),
                IconButton("fa fa-filter", showSearchBox)
              )
            ),
            SearchBox(
              id = s"searchBox-${p.folder.getOrElse("NA").replaceAll("/", "_")}",
              label = "Filter content",
              show = s.showFilter,
              onTextChange = onTextChange
            ),
//            IconView(p.oid, s.files, p.selected, s.filterText, s.ctl)
            TableView(p.oid, s.files, p.selected, s.filterText, s.ctl)
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
      p.showFilter == s.showFilter &&
      p.files.size == s.files.size
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .componentDidMount($ => Callback.ifTrue($.isMounted(), $.backend.loadContent()))
    .componentWillReceiveProps(cwrp =>
      Callback.ifTrue(cwrp.$.isMounted() && cwrp.nextProps.selected.value.isEmpty,
        Callback(cwrp.$.backend.loadContent(cwrp.nextProps)))
    )
    .build

  def apply(
    oid: String,
    folder: Option[String],
    wrappers: Seq[ManagedFile],
    selected: ExternalVar[Option[ManagedFile]],
    ctl: RouterCtl[FolderPath]) = component(Props(oid, folder, wrappers, ctl, Loading, selected))
}
