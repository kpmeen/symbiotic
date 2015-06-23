package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.dman.{DocBrowser, FolderContent}
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  case class Props(customerId: String, projectId: String, selectedFolder: Option[String], ctl: RouterCtl[FolderPath])

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style(
      display.flex,
      height(100.%%)
    )

    val nav = style(
      width(300.px),
      height(100.%%),
      overflow.scroll,
      borderRight :=! "1px solid rgb(223, 220, 220)"
    )
  }

  val component = ReactComponentB[Props]("DocumentManagement")
    .initialStateP(p => p)
    .render { $ =>
    <.div(Style.container,
      <.div(Style.nav,
        DocBrowser($.props.customerId, $.props.projectId, $.props.selectedFolder, $.props.ctl)
      ),
      <.div(Style.container, ^.width := "100%",
        FolderContent($.props.customerId, $.props.selectedFolder, $.props.ctl)
      )
    )
  }.build

  def apply(p: Props): ReactComponentU[Props, Props, Unit, TopNode] = component(p)

  def apply(cid: String, pid: String, ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(cid, pid, None, ctl))

  def apply(cid: String, pid: String, sf: Option[String], ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(cid, pid, sf, ctl))
}
