/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.routes.SymbioticRouter
import org.scalajs.dom.ext.Ajax
import upickle._

import scala.concurrent.ExecutionContext.Implicits.global
import scalacss.Defaults._
import scalacss.ScalaCssReact._

case class FolderItem(folderName: String, children: List[FolderItem])

case class FTree(folders: Seq[FolderItem])

case class Foo(folderName: String)

object DocBrowser {

  object Style extends StyleSheet.Inline {

    import dsl._

    val ulStyle = style(className = "tree-root")(
      listStyleType := "disc",
      paddingLeft(20.px),
      cursor.pointer
    )

  }

  case class Props(cid: String)

  case class State(folders: FTree)

  class Backend(t: BackendScope[Props, State]) {

    def init(): Unit =
      for {
        json <- Ajax.get(
          url = s"${SymbioticRouter.ServerBaseURI}/document/${t.props.cid}/tree",
          headers = Map(
            "Accept" -> "application/json",
            "Content-Type" -> "application/json"
          )
        )
      } yield {
        val ftree = read[FTree](json.responseText)
        t.modState(_.copy(folders = ftree))
      }
  }

  val component = ReactComponentB[Props]("DocBrowser")
    .initialStateP(p => State(FTree(Seq.empty)))
    .backend(new Backend(_))
    .render { (p, s, b) =>
    <.div(^.height := "100%",
      if (s.folders.folders.nonEmpty) {
        <.ul(^.className := Style.ulStyle.className.value,
          s.folders.folders.map(FolderNode(_))
        )
      } else {
        <.span("Could not find any data")
      }
    )
  }.componentWillMount(_.backend.init()).build

  def apply(props: Props) = component(props)

  def apply(cid: String) = component(Props(cid))
}

object FolderNode {

  object Style extends StyleSheet.Inline {

    import dsl._

    val children = boolStyle(expanded => styleS(
      listStyleType := "disc",
      cursor.pointer,
      paddingLeft(20.px),
      textDecoration := "none",
      mixinIfElse(expanded)(
        display.contents
      )(display.none)
    ))
  }

  class Backend(t: BackendScope[CurrNode, CurrNode]) {
    def expandCollapse(e: ReactEventI): Unit = {
      t.modState(_.copy(expanded = !t.state.expanded))
    }
  }

  case class CurrNode(fi: FolderItem, expanded: Boolean)

  val folderNode = ReactComponentB[CurrNode]("FolderItem")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render((p, s, b) =>
    <.li(^.listStyleType := "disc",
      <.a(^.onClick ==> b.expandCollapse, p.fi.folderName),
      <.div(Style.children(s.expanded),
        <.ul(p.fi.children.map(FolderNode(_)))
      )
    )).build

  def apply(p: CurrNode): ReactComponentU[CurrNode, CurrNode, Backend, TopNode] = folderNode(p)

  def apply(fi: FolderItem): ReactComponentU[CurrNode, CurrNode, Backend, TopNode] = folderNode(CurrNode(fi, expanded = false))
}