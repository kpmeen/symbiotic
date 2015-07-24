/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.extra.{ExternalVar, LogLifecycle, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.components.{SearchBox, Spinner}
import net.scalytica.symbiotic.css.{FileTypes, Material, MaterialColors}
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routes.DMan.FolderPath
import net.scalytica.symbiotic.util.{AjaxStatus, Failed, Finished, Loading}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderContent {

  object Style extends StyleSheet.Inline {

    import dsl._

    val fcContainer = Material.container.compose(style(
      width(100.%%)
    ))

    val loading = style(
      Material.valignWrapper,
      height(100.%%),
      width(100.%%)
    )

    val folderContentWrapper = Material.cardContent.compose(style(
      maxHeight(550.px),
      overflowY.scroll
    ))

    val fcGrouping = styleF.bool(selected => styleS(
      Material.centerAlign,
      display.inlineTable,
      marginTop(25.px),
      marginLeft(25.px),
      marginBottom(0.px),
      marginRight(0.px),
      padding(5.px),
      width(120.px),
      height(100.px),
      cursor.pointer,
      mixinIfElse(selected)(
        &.hover(backgroundColor(MaterialColors.IndigoLighten5))
      )(&.hover(backgroundColor.lightcyan)),
      mixinIf(selected)(
        backgroundColor(MaterialColors.BlueLighten5)
      )
    ))

    val folderLabel = style(
      fontSize(14.px),
      wordWrap.breakWord,
      wordBreak.breakAll
    )
  }

  case class Props(
    cid: String,
    folder: Option[String],
    fw: Seq[FileWrapper],
    ctl: RouterCtl[FolderPath],
    status: AjaxStatus,
    selected: ExternalVar[Option[FileWrapper]],
    filterText: String = "")

  class Backend(t: BackendScope[Props, Props]) {
    def loadContent(): Unit = {
      loadContent(t.props)
    }

    def loadContent(p: Props): Unit = {
      FileWrapper.loadF(p.cid, p.folder).onComplete {
        case Success(s) => s match {
          case Right(res) => t.modState(_.copy(folder = p.folder, fw = res, status = Finished))
          case Left(failed) => t.modState(_.copy(folder = p.folder, fw = Nil, status = failed))
        }
        case Failure(err) =>
          log.error(err)
          t.modState(_.copy(folder = p.folder, fw = Nil, status = Failed(err.getMessage)))
      }
      t.modState(_.copy(status = Loading, filterText = ""))
    }

    def changeFolder(fw: FileWrapper): Unit = {
      t.state.ctl.set(FolderPath(UUID.fromString(t.props.cid), fw.path)).unsafePerformIO()
      t.state.selected.set(None).unsafePerformIO()
    }

    def onTextChange(text: String) = {
      t.modState(_.copy(filterText = text))
    }
  }

  implicit val fwReuse = Reusability.fn((p: Props, s: Props) =>
    p.folder == s.folder && p.status == s.status && p.filterText == s.filterText && p.selected.value == s.selected.value
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render { (p, s, b) =>

    def setSelected(fw: FileWrapper): Unit = p.selected.set(Option(fw)).unsafePerformIO()

    def folderContent(contentType: FileTypes.FileType, wrapper: FileWrapper): ReactElement =
      contentType match {
        case FileTypes.Folder =>
          <.div(Style.fcGrouping(false), ^.onClick --> b.changeFolder(wrapper),
            <.i(FileTypes.Styles.Icon3x(FileTypes.Folder)),
            <.a(Style.folderLabel, wrapper.simpleFolderName)
          )
        case _ =>
          <.div(Style.fcGrouping(p.selected.value.contains(wrapper)), ^.onClick --> setSelected(wrapper),
            <.i(FileTypes.Styles.Icon3x(contentType)),
            <.a(^.href := wrapper.downloadLink, <.span(Style.folderLabel, wrapper.filename))
          )
      }

    val wrappers = s.fw.filter { item =>
      val ft = s.filterText.toLowerCase
      item.filename.toLowerCase.contains(ft) || item.simpleFolderName.toLowerCase.contains(ft)
    }
    s.status match {
      case Loading =>
        <.div(Style.fcContainer,
          <.div(Material.cardMedium,
            <.div(Material.cardContent,
              <.div(Style.loading, Spinner(Medium))
            )
          )
        )
      case Finished =>
        <.div(Style.fcContainer,
          PathCrumb(p.cid, p.folder.getOrElse("/"), p.selected, p.ctl),
          <.div(Material.cardDefault,
            <.div(Material.cardContent,
              <.div(Material.row,
                SearchBox(s"searchBox-${p.folder.getOrElse("NA").replaceAll("/", "_")}", "Filter...", onTextChange = b.onTextChange)
              )
            ),
            <.div(Style.folderContentWrapper,
              if (s.fw.nonEmpty) {
                wrappers.map(w =>
                  if (w.isFolder.get) folderContent(FileTypes.Folder, w)
                  else folderContent(FileTypes.fromContentType(w.contentType), w)
                )
              } else {
                <.span("Folder is empty")
              }
            )
          )
        )
      case Failed(err) => <.div(Style.fcContainer, err)
    }
  }
    .configure(Reusability.shouldComponentUpdate)
    .configure(LogLifecycle.short)
    .componentDidMount(csm => if (csm.isMounted()) csm.backend.loadContent())
    .componentWillReceiveProps((csm, p) => if (csm.isMounted() && p.selected.value.isEmpty) csm.backend.loadContent(p))
    .build

  def apply(cid: String, folder: Option[String], wrappers: Seq[FileWrapper], selected: ExternalVar[Option[FileWrapper]], ctl: RouterCtl[FolderPath]) =
    component(Props(cid, folder, wrappers, ctl, Loading, selected))
}