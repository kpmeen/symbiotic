/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.components.{SearchBox, Spinner}
import net.scalytica.symbiotic.css.FileTypes._
import net.scalytica.symbiotic.css.{FontAwesome, Material}
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

    val ctDomain = Domain.ofValues[FileTypes](Folder, GenericFile)

    val fcContainer = Material.container.compose(style(
      height(100.%%),
      width(100.%%)
    ))

    val loading = style(
      Material.valignWrapper,
      height(100.%%),
      width(100.%%)
    )

    val folderContentWrapper = style(
      width.inherit,
      height.inherit
    )

    val fcGrouping = style(
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
      mixin(&.hover(backgroundColor.lightcyan))
    )

    val folderIcon = styleF(ctDomain) { ct =>
      val ctype = ct match {
        case Folder => styleS(FontAwesome.folder)
        case GenericFile => styleS(FontAwesome.file)
      }
      val fa = styleS(
        Material.centerAlign,
        addClassNames("fa-3x"),
        display.block,
        color.lightskyblue
      )
      fa.compose(ctype)
    }

    val folderLabel = style(fontSize(14.px))
  }

  case class Props(
    cid: String,
    folder: Option[String],
    fw: Seq[FileWrapper],
    ctl: RouterCtl[FolderPath],
    status: AjaxStatus,
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
      t.modState(_.copy(status = Loading))
    }

    def changeFolder(fw: FileWrapper): Unit = {
      t.state.ctl.set(
        FolderPath(UUID.fromString(t.props.cid), fw.path)
      ).unsafePerformIO()
    }

    def onTextChange(text: String) = {
      t.modState(_.copy(filterText = text))
    }
  }

  implicit val fwReuse = Reusability.fn((p: Props, s: Props) =>
    p.folder == s.folder && p.status == s.status && p.filterText == s.filterText
  )

  val component = ReactComponentB[Props]("FolderContent")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render { (p, s, b) =>

      def folderContent(contentType: FileTypes, wrapper: FileWrapper): ReactElement =
        contentType match {
          case Folder =>
            <.div(Style.fcGrouping, ^.onClick --> b.changeFolder(wrapper),
              <.i(Style.folderIcon(Folder)),
              <.span(Style.folderLabel, wrapper.simpleFolderName)
            )
          case GenericFile =>
            <.div(Style.fcGrouping,
              <.a(^.href := wrapper.downloadLink, <.i(Style.folderIcon(GenericFile))),
              <.span(Style.folderLabel, wrapper.filename)
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
            PathCrumb(p.cid, p.folder.getOrElse("/"), p.ctl),
            <.div(Material.cardDefault,
              <.div(Material.cardContent,
                <.div(Material.row,
                  SearchBox(s"searchBox-${p.folder.getOrElse("NA").replaceAll("/", "_")}", "Filter...", onTextChange = b.onTextChange)
                )
              ),
              <.div(Material.cardContent,
                if (s.fw.nonEmpty) {
                  wrappers.map(w =>
                    if (w.isFolder.get) folderContent(Folder, w)
                    else folderContent(GenericFile, w)
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
    .componentDidMount(csm => if (csm.isMounted()) csm.backend.loadContent())
    .componentWillReceiveProps((csm, p) => if (csm.isMounted()) csm.backend.loadContent(p))
    .build

  def apply(p: Props) = component(p)

  def apply(cid: String, folder: Option[String], ctl: RouterCtl[FolderPath]) =
    component(Props(cid, folder, Nil, ctl, Loading))

  def apply(cid: String, folder: Option[String], fw: Seq[FileWrapper], ctl: RouterCtl[FolderPath]) =
    component(Props(cid, folder, fw, ctl, Loading))
}