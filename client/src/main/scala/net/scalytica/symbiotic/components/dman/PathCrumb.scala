/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.FontIcons
import net.scalytica.symbiotic.models.dman.{FTree, ManagedFile}
import net.scalytica.symbiotic.routing.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object PathCrumb {

  object Style extends StyleSheet.Inline {

  }

  case class Props(oid: String, path: String, selected: ExternalVar[Option[ManagedFile]], routerCtl: RouterCtl[FolderPath])

  class Backend(t: BackendScope[Props, Props]) {
    def changePage(path: Option[String]): Callback = {
      t.props.flatMap(p => p.routerCtl.set(FolderPath(UUID.fromString(p.oid), path))) >>
        t.state.flatMap(_.selected.set(None))
    }

    case class PathSegment(segment: String, path: String)

    def pathTag(path: Option[String], displayValue: ReactTag): ReactTag =
      <.li(<.a(^.cursor := "pointer", ^.onClick --> changePage(path))(displayValue))

    def pathTags(elems: Seq[String]): Seq[TagMod] = {
      var pb = Seq.newBuilder[String]
      val paths = elems.map { e =>
        if (e.nonEmpty) {
          pb += e
          val curr = pb.result()
          Some(PathSegment(e, curr.mkString("/", "/", "")))
        } else None
      }.takeRight(CrumbLimit).filter(_.nonEmpty).map(_.get)
      paths.zipWithIndex.map { path =>
        if (paths.size == CrumbLimit && path._2 == 0) pathTag(Option(path._1.path), <.span("..."))
        else pathTag(Option(path._1.path), <.span(path._1.segment.stripPrefix("/")))
      }
    }

    def render(p: Props) = {
      val pElems: Seq[String] = p.path.stripPrefix(FTree.rootFolder).stripPrefix("/").stripSuffix("/").split("/")

      <.ol(^.className := "breadcrumb",
        if (pElems.nonEmpty) pathTag(None, <.i(FontIcons.hddDrive)).compose(pathTags(pElems))
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

  def apply(oid: String, path: String, selected: ExternalVar[Option[ManagedFile]], ctl: RouterCtl[FolderPath]) =
    component(Props(oid, path, selected, ctl))

}
