/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.extra.{ExternalVar, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.components.{IconButton, SearchBox, Spinner}
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderPath
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLFormElement
import org.scalajs.jquery._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderContent {

  object Style extends StyleSheet.Inline {

    import dsl._

    val loading = style("filecontent-loading")(
      addClassNames("center-block", "text-center"),
      height(100.%%),
      width(100.%%)
    )

    val contentPanel = style("content-panel")(
      addClassNames("panel", "panel-default"),
      marginTop(10.px),
      boxShadow := "none",
      borderRadius.`0`,
      border.`0`
    )

    val contentPanelBody = style("content-panel-body")(
      addClassNames("panel-body"),
      padding.`0`
    )

    val fcGrouping = styleF.bool(selected => styleS(
      addClassNames("center-block", "text-center"),
      display.inlineTable,
      padding(5.px),
      margin(5.px),
      width(120.px),
      height(100.px),
      cursor.pointer,
      mixinIfElse(selected)(
        backgroundColor.rgb(190, 220, 230)
      )(&.hover(
        backgroundColor.rgb(222, 222, 222)),
        textDecoration := "none"
      )
    ))

    val folder = style(color.steelblue)
    val file = style(color.lightslategrey)

    val folderLabel = style(
      fontSize(14.px),
      color.darkslategrey,
      wordWrap.breakWord,
      wordBreak.breakAll
    )
  }

  case class Props(
    oid: String,
    folder: Option[String],
    fw: Seq[ManagedFile],
    ctl: RouterCtl[FolderPath],
    status: AjaxStatus,
    selected: ExternalVar[Option[ManagedFile]],
    showFilter: Boolean = false,
    filterText: String = "")

  class Backend($: BackendScope[Props, Props]) {
    /**
     * Default content loading...
     */
    def loadContent(): Callback = $.props.map(p => loadContent(p))

    /**
     * Load content based on props...
     */
    def loadContent(p: Props) =
      ManagedFile.load(p.oid, p.folder).map {
        case Right(res) => $.modState(_.copy(folder = p.folder, fw = res, status = Finished))
        case Left(failed) => $.modState(_.copy(folder = p.folder, fw = Nil, status = failed))
      }.recover {
        case err =>
          log.error(err)
          $.modState(_.copy(folder = p.folder, fw = Nil, status = Failed(err.getMessage)))
      }.map(_.runNow())

    /**
     * Navigate to a different folder...
     */
    def changeFolder(fw: ManagedFile): Callback =
      $.state.flatMap { s =>
        $.props.flatMap(p => s.ctl.set(FolderPath(UUID.fromString(p.oid), fw.path))) >>
          s.selected.set(None)
      }

    /**
     * Handle text change in the search/filter component
     */
    def onTextChange(text: String): Callback = $.modState(_.copy(filterText = text))

    /**
     * Mark the given file as (de-)selected
     */
    def setSelected(fw: ManagedFile): Callback =
      $.props.flatMap { p =>
        if (p.selected.value.contains(fw)) p.selected.set(None)
        else p.selected.set(Option(fw))
      }

    /**
     * Triggers the default HTML5 file upload dialogue
     */
    def showFileUploadDialogue(e: ReactEventI): Callback = Callback(jQuery("#ManagedFileUplad").click())

    /**
     * Uploads a managed file...
     */
    def uploadFile(e: ReactEventI): Callback = {
      CallbackTo {
        val state = $.accessDirect.state
        $.props.map { props =>
          val form = e.currentTarget.parentElement.asInstanceOf[HTMLFormElement]
          val file = e.currentTarget.files.item(0)
          ManagedFile.upload(props.oid, props.folder.getOrElse("/"), form) {
            Callback {
              loadContent().runNow()
            }
          }
        }
      }.flatten
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

    /**
     * Renders a ManagedFile as either a File or a Folder depending on its FileType
     */
    def folderContent(selected: Option[ManagedFile], contentType: FileTypes.FileType, wrapper: ManagedFile): ReactElement =
      contentType match {
        case FileTypes.Folder =>
          <.div(Style.fcGrouping(false), ^.onClick --> changeFolder(wrapper),
            <.i(FileTypes.Styles.Icon3x(FileTypes.Folder).compose(Style.folder)),
            <.a(Style.folderLabel, wrapper.filename)
          )
        case _ =>
          <.div(Style.fcGrouping(selected.contains(wrapper)), ^.onClick --> setSelected(wrapper),
            <.i(FileTypes.Styles.Icon3x(contentType).compose(Style.file)),
            <.a(^.id := wrapper.id, ^.href := wrapper.downloadLink, <.span(Style.folderLabel, wrapper.filename))
          )
      }

    def render(p: Props, s: Props): vdom.ReactTagOf[Div] = {
      val wrappers = s.fw.filter { item =>
        val ft = s.filterText.toLowerCase
        item.filename.toLowerCase.contains(ft)
      }.map(w =>
        if (w.metadata.isFolder.get) folderContent(p.selected.value, FileTypes.Folder, w)
        else folderContent(p.selected.value, FileTypes.fromContentType(w.contentType), w)
      )

      s.status match {
        case Loading =>
          <.div(^.className := "container-fluid",
            <.div(^.className := "panel panel-default",
              <.div(^.className := "panel-body",
                <.div(Style.loading, Spinner(Medium))
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
            <.div(^.className :="btn-toolbar",
              <.div(^.className := "btn-group", ^.role := "group",
                IconButton("fa fa-folder" /*TODO: Call add folder service*/),
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

            <.div(Style.contentPanel,
              <.div(Style.contentPanelBody,
                <.div(^.className := "container-fluid",
                  if (s.fw.nonEmpty) wrappers
                  else <.span("Folder is empty")
                )
              )
            )
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
      p.fw.size == s.fw.size
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
//    .configure(LogLifecycle.short)
    .componentDidMount($ =>
      Callback.ifTrue($.isMounted(), $.backend.loadContent())
    )
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
