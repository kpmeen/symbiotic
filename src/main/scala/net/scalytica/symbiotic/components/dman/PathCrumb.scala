/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object PathCrumb {

  object Style extends StyleSheet.Inline {

    import dsl._

    val separator = style(fontWeight.bold)

    val card = style(addClassNames("card"))
    val cardContent = style(addClassNames("card-content"))
    val pathSegment = style(
      cursor.pointer
    )

  }

  case class Props(cid: String, path: String, routerCtl: RouterCtl[FolderPath])

  class Backend(t: BackendScope[Props, Props]) {
    def changePage(path: String): Unit = {
      t.props.routerCtl.set(FolderPath(UUID.fromString(t.props.cid), Option(path))).unsafePerformIO()
    }
  }

  val component = ReactComponentB[Props]("PathCrumb")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render { (p, s, b) =>

      def separator(): ReactElement = <.span(Style.separator, "/")

      def pathElements(elems: Seq[String]): Seq[TagMod] = {
        var pb = Seq.newBuilder[String]
        elems.map { e =>
          pb += e
          val curr = pb.result()
          <.a(Style.pathSegment, ^.onClick --> b.changePage(curr.mkString("/", "/", "")))(e).compose(separator())
        }
      }

      val pElems = p.path.stripPrefix("/").stripSuffix("/").split("/")
      <.div(Style.card)(
        <.div(Style.cardContent)(
          separator(),
          pathElements(pElems)
        )
      )
    }.build

  def apply(p: Props) = component(p)

  def apply(cid: String, path: String, ctl: RouterCtl[FolderPath]) = component(Props(cid, path, ctl))

}
