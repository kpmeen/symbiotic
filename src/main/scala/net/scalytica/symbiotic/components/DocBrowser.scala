/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.routes.SymbioticRouter
import org.scalajs.dom.ext.Ajax
import upickle._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalacss.Defaults._

case class FolderItem(folderName: String, children: List[FolderItem])

case class FTree(folders: Seq[FolderItem])

case class Foo(folderName: String)

object DocBrowser {

  object Style extends StyleSheet.Inline {

    //    val ulStyle = style(unsafeRoot("ul")(
    //      listStyleType := "disc"
    //    ))

  }

  case class Props(cid: String, visible: Boolean)

  case class State(folders: FTree, visible: Boolean)

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
    .initialStateP(p => State(FTree(Seq.empty), p.visible))
    .backend(new Backend(_))
    .render { (p, s, b) =>
    <.div(^.height := "100%",
      if (s.folders.folders.nonEmpty) {
        <.ul(^.listStyleType := "disc", ^.paddingLeft := "20px",
          s.folders.folders.map(FolderNode(_))
        )
      } else {
        <.span("Could not find any data")
      }
    )
  }.componentWillMount(_.backend.init())
    .build

  def apply(props: Props) = component(props)

  def apply(cid: String) = component(Props(cid, visible = false))
}

object FolderNode {

  val folderNode = ReactComponentB[FolderItem]("FolderItem")
    .render((item: FolderItem) =>
    <.li(^.listStyleType := "disc",
      <.span(item.folderName),
      <.ul(^.listStyleType := "disc", ^.paddingLeft := "20px",
        item.children.map(FolderNode(_))
      )
    )).build

  def apply(fi: FolderItem): ReactComponentU[FolderItem, Unit, Unit, TopNode] = folderNode(fi)
}