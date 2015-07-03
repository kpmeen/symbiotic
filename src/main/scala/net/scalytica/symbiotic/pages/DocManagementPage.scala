package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.dman.{FolderTree, FolderContent}
import net.scalytica.symbiotic.css.Material
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  case class Props(customerId: String, projectId: String, selectedFolder: Option[String], ctl: RouterCtl[FolderPath])

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = Material.row.compose(style(
      display.flex,
      height(100.%%),
      width(100.%%)
    ))

    val tree = style(
      Material.col,
      addClassName("s3"),
      display.flex,
      height(100.%%),
      overflow.scroll
    )

    val content = style(
      Material.col,
      addClassNames("s9"),
      display.flex,
      height(100.%%)
    )
  }

  val component = ReactComponentB[Props]("DocumentManagement")
    .initialStateP(p => p)
    .render { $ =>
    <.div(Style.container,
      <.div(Style.tree, FolderTree($.props.customerId, $.props.projectId, $.props.selectedFolder, $.props.ctl)),
      <.div(Style.content, FolderContent($.props.customerId, $.props.selectedFolder, $.props.ctl))
    )
  }.build

  def apply(p: Props): ReactComponentU[Props, Props, Unit, TopNode] = component(p)

  def apply(cid: String, pid: String, ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(cid, pid, None, ctl))

  def apply(cid: String, pid: String, sf: Option[String], ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(cid, pid, sf, ctl))
}
