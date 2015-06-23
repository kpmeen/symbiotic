/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalacss.Defaults._

object DocBrowser {

  object Style extends StyleSheet.Inline {

    import dsl._

    val ulStyle = style(className = "tree-root")(
      paddingLeft(20.px),
      cursor.pointer
    )

  }

  case class Props(cid: String, pid: String, selectedFolder: Option[String], ctl: RouterCtl[FolderPath])

  case class State(ftree: FTree, selectedFolder: Option[String])

  class Backend(t: BackendScope[Props, State]) {

    def init(): Unit = {
      for {
        ftree <- FTree.load(t.props.cid)
      } yield t.modState(_.copy(ftree = ftree))
    }
  }

  val component = ReactComponentB[Props]("DocBrowser")
    .initialStateP(p => State(FTree(Seq.empty), p.selectedFolder))
    .backend(new Backend(_))
    .render { (p, s, b) =>
    <.div(^.height := "100%", ^.width := "100%",
      if (s.ftree.folders.nonEmpty) {
        <.ul(^.className := Style.ulStyle.className.value,
          s.ftree.folders.map(fitem => FolderNode(fi = fitem, sf = s.selectedFolder, ctl = p.ctl))
        )
      } else {
        <.span("Could not find any data")
      }
    )
  }.componentWillMount(_.backend.init()).build

  def apply(props: Props) = component(props)

  def apply(cid: String, pid: String, sf: Option[String], ctl: RouterCtl[FolderPath]) = component(Props(cid, pid, sf, ctl))
}