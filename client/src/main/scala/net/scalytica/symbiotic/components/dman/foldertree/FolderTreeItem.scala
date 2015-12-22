/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldertree

import java.util.UUID

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.css.FileTypes.{Folder, FolderOpen}
import net.scalytica.symbiotic.css.{FileTypes, GlobalStyle}
import net.scalytica.symbiotic.models.OrgId
import net.scalytica.symbiotic.models.dman.{FTree, FolderItem, ManagedFile}
import net.scalytica.symbiotic.routing.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderTreeItem {

  object Style extends StyleSheet.Inline {

    import dsl._

    val children = styleF.bool(expanded => styleS(
      cursor.pointer,
      textDecoration := "none",
      mixinIfElse(expanded)(
        display.contents
      )(display.none)
    ))

    val folderWrapper = style(
      display.inlineFlex,
      lineHeight(2.em),
      mixin(&.hover(backgroundColor.rgb(222, 222, 222)))
    )

    val folder = styleF.bool(expanded => styleS(
      color.steelblue,
      mixinIfElse(expanded)(
        FileTypes.Styles.Icon2x(FolderOpen)
      )(FileTypes.Styles.Icon2x(Folder))
    ))

    val folderName = style(
      color.darkslategrey,
      marginLeft(5.px),
      fontSize(16.px),
      mixin(&.hover(
        textDecoration := "none"
      ))
    )
  }

  case class Props(
    oid: OrgId,
    fi: FolderItem,
    selectedFolder: Option[String],
    selectedFile: ExternalVar[Option[ManagedFile]],
    expanded: Boolean,
    ctl: RouterCtl[FolderPath])

  class Backend($: BackendScope[Props, Props]) {
    def expandCollapse(e: ReactEventI): Callback =
      $.modState(s => s.copy(expanded = !s.expanded))

    def changeFolder(e: ReactEventI): Callback =
      $.state.flatMap { s =>
        s.selectedFile.set(None) >>
          $.props.flatMap(p => s.ctl.set(FolderPath(p.oid.toUUID, Option(p.fi.path))))
      }

    def render(p: Props, s: Props) = {
      <.li(
        <.div(Style.folderWrapper,
          <.i(Style.folder(s.expanded), ^.onClick ==> expandCollapse),
          <.a(Style.folderName, ^.onClick ==> changeFolder, s" ${p.fi.name}")
        ),
        <.div(Style.children(s.expanded),
          <.ul(GlobalStyle.ulStyle(false), ^.listStyle := "none", p.fi.children.map(fi =>
            FolderTreeItem(p.oid, fi, p.selectedFolder, p.selectedFile, p.ctl))
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("FolderTreeItem")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .build

  // ===============  Constructors ===============

  type FolderTreeItemComponent = ReactComponentU[Props, Props, Backend, TopNode]

  def apply(p: Props): FolderTreeItemComponent = component(p)

  def apply(oid: OrgId, fi: FolderItem, sfolder: Option[String], sfile: ExternalVar[Option[ManagedFile]], ctl: RouterCtl[FolderPath]): FolderTreeItemComponent = {
    if (fi.path == FTree.rootFolder) component(Props(oid, fi, sfolder, sfile, expanded = true, ctl))
    else component(Props(oid, fi, sfolder, sfile, expanded = false, ctl))
  }

}