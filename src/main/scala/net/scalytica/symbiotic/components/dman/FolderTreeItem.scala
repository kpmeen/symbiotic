/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import java.util.UUID

import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.css.{FontAwesome, Material}
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman.FolderItem
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderTreeItem {

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

    val folderWrapper = style(
      display.inlineFlex,
      lineHeight(2.em),
      mixin(&.hover(backgroundColor.lightcyan))
    )

    val folder = styleF.bool(expanded => styleS(
      mixinIfElse(expanded)(FontAwesome.folderOpen)(FontAwesome.folder),
      addClassName("fa-2x"),
      color.lightskyblue
    ))

    val folderName = style(
      Material.truncate,
      marginLeft(5.px),
      fontSize(16.px)
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

  val component = ReactComponentB[Props]("FolderTreeItem")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render((p, s, b) =>
      <.li(
        <.div(Style.folderWrapper,
          <.i(Style.folder(s.expanded), ^.onClick ==> b.expandCollapse),
          <.a(Style.folderName, ^.onClick ==> b.changeFolder, s" ${p.fi.folderName}")
        ),
        <.div(Style.children(s.expanded),
          <.ul(p.fi.children.map(fi => FolderTreeItem(fi, p.selectedFolder, p.ctl)))
        )
      )).build

  // ===============  Constructors ===============

  type FolderTreeItemComponent = ReactComponentU[Props, Props, Backend, TopNode]

  def apply(p: Props): FolderTreeItemComponent = component(p)

  def apply(fi: FolderItem, sf: Option[String], ctl: RouterCtl[FolderPath]): FolderTreeItemComponent =
    component(Props(fi, sf, expanded = false, ctl))
}