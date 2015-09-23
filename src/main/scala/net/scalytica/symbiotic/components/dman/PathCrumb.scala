/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.FontAwesome
import net.scalytica.symbiotic.models.dman.File
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object PathCrumb {

  object Style extends StyleSheet.Inline {

  }

  case class Props(oid: String, path: String, selected: ExternalVar[Option[File]], routerCtl: RouterCtl[FolderPath])

  class Backend(t: BackendScope[Props, Props]) {
    def changePage(path: Option[String]): Unit = {
      t.props.routerCtl.set(FolderPath(UUID.fromString(t.props.oid), path)).unsafePerformIO()
      t.state.selected.set(None).unsafePerformIO()
    }
  }

  private[this] val CrumbLimit = 8

  val component = ReactComponentB[Props]("PathCrumb")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render { (p, s, b) =>

      case class PathSegment(segment: String, path: String)

      def pathTag(path: Option[String], displayValue: ReactTag): ReactTag =
        <.li(<.a(^.onClick --> b.changePage(path))(displayValue))

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
          println(s"The path is: $path")
          if (paths.size == CrumbLimit && path._2 == 0) pathTag(Option(path._1.path), <.span("..."))
          else pathTag(Option(path._1.path), <.span(path._1.segment.stripPrefix("/")))
        }
      }

      val pElems: Seq[String] = p.path.stripPrefix("/root/").stripPrefix("/").stripSuffix("/").split("/")

      <.ol(^.className := "breadcrumb",
        if (pElems.nonEmpty) pathTag(None, <.i(FontAwesome.hddDrive)).compose(pathTags(pElems))
        else
          pathTag(None, <.i(FontAwesome.hddDrive))
      )
    }.build

  def apply(p: Props) = component(p)

  def apply(oid: String, path: String, selected: ExternalVar[Option[File]], ctl: RouterCtl[FolderPath]) =
    component(Props(oid, path, selected, ctl))

}
