/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, ReactElement, BackendScope}
import net.scalytica.symbiotic.components.dman.foldercontent.FolderContent_PS.Props
import net.scalytica.symbiotic.core.converters.{SizeConverters, DateConverters}
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object TableView {

  object Style extends StyleSheet.Inline {

    import dsl._

    val row = styleF.bool(selected => styleS(
      mixinIfElse(selected)(
        backgroundColor.rgb(190, 220, 230).important
      )(&.hover(
        backgroundColor.rgb(222, 222, 222).important),
        textDecoration := "none"
      )
    ))

  }

  class Backend(val $: BackendScope[Props, Unit]) extends FolderContentBackend {

    override def renderFile(selected: Option[ManagedFile], contentType: FileTypes.FileType, wrapper: ManagedFile): ReactElement =
      <.tr(Style.row(selected.contains(wrapper)), ^.onClick --> setSelected(wrapper),
        <.td(
          <.i(FileTypes.Styles.IconLg(contentType).compose(FolderContentStyle.file(false))),
          <.span(s" ${wrapper.filename}")
        ),
        <.td(s"${wrapper.uploadDate.map(DateConverters.toReadableDate).getOrElse("-")}"),
        <.td(s"${wrapper.length.map(SizeConverters.toReadableSize).getOrElse("-")}"),
        <.td(wrapper.metadata.version)
      )

    override def renderFolder(selected: Option[ManagedFile], wrapper: ManagedFile): ReactElement =
      <.tr(Style.row(false), ^.onClick --> changeFolder(wrapper),
        <.td(
          <.i(FileTypes.Styles.IconLg(FileTypes.Folder).compose(FolderContentStyle.folder(false))),
          <.span(s" ${wrapper.filename}")
        ),
        <.td("-"),
        <.td("-"),
        <.td("-")
      )

    def render(p: Props) =
      <.div(FolderContentStyle.contentPanel,
        <.div(FolderContentStyle.contentPanelBody,
          <.table(^.className := "table table-striped",
            <.thead(
              <.tr(
                <.th("Name"),
                <.th("Date Added"),
                <.th("Size"),
                <.th("Version")
              )
            ),
            <.tbody(
              if (p.files.nonEmpty) renderContent(p)
              else <.span("Folder is empty")
            )
          )
        )
      )

  }

  val component = ReactComponentB[Props]("TableView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(
    oid: String,
    files: Seq[ManagedFile],
    selected: ExternalVar[Option[ManagedFile]],
    filterText: String = "",
    ctl: RouterCtl[FolderPath]) = component(Props(oid, files, selected, filterText, ctl))

}
