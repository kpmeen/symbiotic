/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.extra.{ExternalVar, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.components.{SearchBox, Spinner}
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderPath

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}
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

  class Backend(t: BackendScope[Props, Props]) {
    def loadContent(): Unit = loadContent(t.props)

    def loadContent(p: Props): Unit = {
      File.loadF(p.oid, p.folder).onComplete {
        case Success(s) => s match {
          case Right(res) => t.modState(_.copy(folder = p.folder, fw = res, status = Finished))
          case Left(failed) => t.modState(_.copy(folder = p.folder, fw = Nil, status = failed))
        }
        case Failure(err) =>
          log.error(err)
          t.modState(_.copy(folder = p.folder, fw = Nil, status = Failed(err.getMessage)))
      }
      //      t.modState(_.copy(status = Loading, filterText = ""))
    }

    def changeFolder(fw: File): Unit = {
      t.state.ctl.set(FolderPath(UUID.fromString(t.props.oid), fw.path)).unsafePerformIO()
      t.state.selected.set(None).unsafePerformIO()
    }

    def onTextChange(text: String) = {
      t.modState(_.copy(filterText = text))
    }
  }

  implicit val fwReuse = Reusability.fn((p: Props, s: Props) =>
    p.folder == s.folder &&
      p.status == s.status &&
      p.filterText == s.filterText &&
      p.selected.value == s.selected.value
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render { (p, s, b) =>

      def setSelected(fw: File): Unit = p.selected.set(Option(fw)).unsafePerformIO()

      def folderContent(contentType: FileTypes.FileType, wrapper: File): ReactElement =
        contentType match {
          case FileTypes.Folder =>
            <.div(Style.fcGrouping(false), ^.onClick --> b.changeFolder(wrapper),
              <.i(FileTypes.Styles.Icon3x(FileTypes.Folder).compose(Style.folder)),
              <.a(Style.folderLabel, wrapper.simpleFolderName)
            )
          case _ =>
            <.div(Style.fcGrouping(p.selected.value.contains(wrapper)), ^.onClick --> setSelected(wrapper),
              <.i(FileTypes.Styles.Icon3x(contentType).compose(Style.file)),
              <.a(^.href := wrapper.downloadLink, <.span(Style.folderLabel, wrapper.filename))
            )
        }

      val wrappers = s.fw.filter { item =>
        val ft = s.filterText.toLowerCase
        item.filename.toLowerCase.contains(ft) || item.simpleFolderName.toLowerCase.contains(ft)
      }
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
            SearchBox(s"searchBox-${p.folder.getOrElse("NA").replaceAll("/", "_")}", "Filter content", onTextChange = b.onTextChange),
            <.div(^.className := "panel panel-default",
              <.div(^.className := "panel-body",
                <.div(^.className := "container-fluid",
                  if (s.fw.nonEmpty) {
                    wrappers.map(w =>
                      if (w.metadata.isFolder.get) folderContent(FileTypes.Folder, w)
                      else folderContent(FileTypes.fromContentType(w.contentType), w)
                    )
                  } else {
                    <.span("Folder is empty")
                  }
                )
              )
            )
          )
        case Failed(err) => <.div(^.className := "container-fluid", err)
      }
    }
    .configure(Reusability.shouldComponentUpdate)
//    .configure(LogLifecycle.short)
    .componentDidMount(csm => if (csm.isMounted()) csm.backend.loadContent())
    .componentWillReceiveProps((csm, p) => if (csm.isMounted() && p.selected.value.isEmpty) csm.backend.loadContent(p))
    .build

  def apply(oid: String, folder: Option[String], wrappers: Seq[File], selected: ExternalVar[Option[File]], ctl: RouterCtl[FolderPath]) =
    component(Props(oid, folder, wrappers, ctl, Loading, selected))
}