package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.FontIcons
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object PathCrumb {

  object Style extends StyleSheet.Inline {}

  case class Props(
      selectedFolder: ExternalVar[Option[FileId]],
      pathElements: Seq[(FileId, String)],
      selectedFile: ExternalVar[Option[ManagedFile]],
      ctl: RouterCtl[FolderURIElem]
  )

  class Backend($ : BackendScope[Props, Props]) {

    def changePage(maybeFolderId: Option[FileId]): Callback =
      $.state.flatMap { s =>
        $.props.flatMap { p =>
          p.selectedFile.set(None) >>
            p.selectedFolder.set(maybeFolderId) >>
            s.ctl.set(FolderURIElem(maybeFolderId.map(_.toUUID)))
        }
      }

    case class PathSegment(fid: FileId, segment: String, path: String)

    def pathTag(
        pathSegment: Option[PathSegment],
        displayValue: ReactTag
    ): ReactTag =
      <.li(
        <.a(
          ^.cursor := "pointer",
          ^.onClick --> changePage(pathSegment.map(_.fid))
        )(displayValue)
      )

    def pathTags(elems: Seq[(FileId, String)]): Seq[TagMod] = {
      var pb = Seq.newBuilder[String]
      val paths = elems
        .filterNot(_._2 == "root")
        .map { e =>
          val fid = e._1
          val seg = e._2
          if (seg.nonEmpty) {
            pb += seg
            val curr = pb.result()
            Some(PathSegment(fid, seg, curr.mkString("/", "/", "")))
          } else None
        }
        .takeRight(CrumbLimit)
        .filter(_.nonEmpty)
        .map(_.get)
      paths.zipWithIndex.map { path =>
        if (paths.size == CrumbLimit && path._2 == 0)
          pathTag(Option(path._1), <.span("..."))
        else pathTag(Option(path._1), <.span(path._1.segment.stripPrefix("/")))
      }
    }

    def render(p: Props) = {
      val pElems = p.pathElements
      <.ol(
        ^.className := "breadcrumb",
        if (pElems.nonEmpty)
          pathTag(None, <.i(FontIcons.hddDrive)).compose(pathTags(pElems))
        else
          pathTag(None, <.i(FontIcons.hddDrive))
      )
    }
  }

  private[this] val CrumbLimit = 8

  val component = ReactComponentB[Props]("PathCrumb")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)

  def apply(
      selectedFolder: ExternalVar[Option[FileId]],
      pathElements: Seq[(FileId, String)],
      selectedFile: ExternalVar[Option[ManagedFile]],
      ctl: RouterCtl[FolderURIElem]
  ) =
    component(Props(selectedFolder, pathElements, selectedFile, ctl))

}
