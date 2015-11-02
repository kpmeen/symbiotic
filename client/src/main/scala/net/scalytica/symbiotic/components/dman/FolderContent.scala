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
import net.scalytica.symbiotic.components.{SearchBox, Spinner, UploadForm}
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderPath
import org.scalajs.dom.html.Div

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

    val fcGrouping = styleF.bool(selected => styleS(
      addClassNames("center-block", "text-center"),
      display.inlineTable,
      padding(5.px),
      margin(10.px),
      width(120.px),
      height(100.px),
      cursor.pointer,
      mixin(&.hover(
        backgroundColor.rgb(222, 222, 222)),
        textDecoration := "none"
      ),
      mixinIf(selected)(
        backgroundColor.rgb(233, 233, 233)
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
    fw: Seq[File],
    ctl: RouterCtl[FolderPath],
    status: AjaxStatus,
    selected: ExternalVar[Option[File]],
    filterText: String = "")

  class Backend($: BackendScope[Props, Props]) {
    def loadContent(): Callback = $.props.map(p => loadContent(p))

    def loadContent(p: Props) = {
      File.load(p.oid, p.folder).map {
        case Right(res) =>
          $.modState(_.copy(folder = p.folder, fw = res, status = Finished))

        case Left(failed) =>
          $.modState(_.copy(folder = p.folder, fw = Nil, status = failed))

      }.recover {
        case err =>
          log.error(err)
          $.modState(_.copy(folder = p.folder, fw = Nil, status = Failed(err.getMessage)))
      }.map(_.runNow())
    }

    def changeFolder(fw: File): Callback =
      $.state.flatMap { s =>
        $.props.flatMap(p => s.ctl.set(FolderPath(UUID.fromString(p.oid), fw.path))) >>
          s.selected.set(None)
      }

    def onTextChange(text: String): Callback = {
      $.modState(_.copy(filterText = text))
    }

    def setSelected(fw: File): Callback = $.props.flatMap(_.selected.set(Option(fw)))

    def folderContent(selected: Option[File], contentType: FileTypes.FileType, wrapper: File): ReactElement =
      contentType match {
        case FileTypes.Folder =>
          <.div(Style.fcGrouping(false), ^.onClick --> changeFolder(wrapper),
            <.i(FileTypes.Styles.Icon3x(FileTypes.Folder).compose(Style.folder)),
            <.a(Style.folderLabel, wrapper.filename)
          )
        case _ =>
          <.div(Style.fcGrouping(selected.contains(wrapper)), ^.onClick --> setSelected(wrapper),
            <.i(FileTypes.Styles.Icon3x(contentType).compose(Style.file)),
            <.a(^.href := wrapper.downloadLink, <.span(Style.folderLabel, wrapper.filename))
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
            PathCrumb(p.oid, p.folder.getOrElse("/"), p.selected, p.ctl),
            UploadForm(p.oid, p.folder, loadContent),
            SearchBox(s"searchBox-${p.folder.getOrElse("NA").replaceAll("/", "_")}", "Filter content", onTextChange = onTextChange),
            <.div(^.className := "panel panel-default",
              <.div(^.className := "panel-body",
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
    wrappers: Seq[File],
    selected: ExternalVar[Option[File]],
    ctl: RouterCtl[FolderPath]) = component(Props(oid, folder, wrappers, ctl, Loading, selected))
}
