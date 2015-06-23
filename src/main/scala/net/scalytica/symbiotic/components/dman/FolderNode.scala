/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react.{ReactComponentB, _}
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.models.dman.FolderItem
import net.scalytica.symbiotic.routes.DMan.FolderPath
import net.scalytica.symbiotic.logger.log

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderNode {

  object Style extends StyleSheet.Inline {

    import dsl._

    val children = styleF.bool(expanded => styleS(
      cursor.pointer,
      paddingLeft(20.px),
      textDecoration := "none",
      mixinIfElse(expanded)(
        display.contents
      )(display.none)
    ))

    val folder = styleF.bool(expanded => styleS(
      addClassName("fa"),
      fontSize(18.px),
      mixinIfElse(expanded)(addClassNames("fa-folder-open"))(addClassNames("fa-folder"))
    ))

    val folderName = style(
      fontSize(18.px)
    )
  }

  class Backend(t: BackendScope[Props, Props]) {
    def expandCollapse(e: ReactEventI): Unit =
      t.modState(_.copy(expanded = !t.state.expanded))

    def changeFolder(e: ReactEventI): Unit = {
      log.info(t.props.fi.fullPath)
      t.state.ctl.set(
        FolderPath(UUID.fromString(t.props.fi.cid), Option(t.props.fi.fullPath))
      ).unsafePerformIO()
    }
  }

  case class Props(fi: FolderItem, selectedFolder: Option[String], expanded: Boolean, ctl: RouterCtl[FolderPath])

  val component = ReactComponentB[Props]("FolderItem")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render((p, s, b) =>
    <.li(
      <.div(
        <.i(Style.folder(s.expanded), ^.onClick ==> b.expandCollapse),
        <.a(Style.folderName, ^.onClick ==> b.changeFolder, s" ${p.fi.folderName}")
      ),
      <.div(Style.children(s.expanded),
        <.ul(p.fi.children.map(fi => FolderNode(fi, p.selectedFolder, p.ctl)))
      )
    )).build

  // ===============  Constructors ===============

  type FolderNodeComponent = ReactComponentU[Props, Props, Backend, TopNode]

  def apply(p: Props): FolderNodeComponent = component(p)

  def apply(fi: FolderItem, sf: Option[String], ctl: RouterCtl[FolderPath]): FolderNodeComponent =
    component(Props(fi, sf, expanded = false, ctl))
}