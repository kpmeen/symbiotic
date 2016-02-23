/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import net.scalytica.symbiotic.components.dman.foldercontent.ContentView_PS.Props
import net.scalytica.symbiotic.core.converters.{DateConverters, SizeConverters}
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

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

  class Backend(val $: BackendScope[Props, Props]) extends ContentViewBackend {

    override def renderFile(selected: Option[ManagedFile], contentType: FileTypes.FileType, wrapper: ManagedFile): ReactElement =
      <.tr(Style.row(selected.contains(wrapper)), ^.onClick --> setSelected(wrapper),
        <.td(^.className := "text-center",
          wrapper.metadata.lock.map(l => <.i(^.className := "fa fa-lock"))
        ),
        <.td(
          <.i(FileTypes.Styles.IconLg(contentType).compose(FolderContentStyle.file(false))),
          <.a(^.id := wrapper.metadata.fid, ^.href := wrapper.downloadLink,
            s" ${wrapper.filename}"
          )
        ),
        <.td(^.className := "text-center",
          s"${wrapper.uploadDate.map(DateConverters.toReadableDate).getOrElse("-")}"
        ),
        <.td(^.className := "text-center",
          s"${wrapper.length.map(SizeConverters.toReadableSize).getOrElse("-")}"
        ),
        <.td(^.className := "text-center", wrapper.metadata.version)
      )

    override def renderFolder(selected: Option[ManagedFile], wrapper: ManagedFile): ReactElement =
      <.tr(Style.row(false), ^.onClick --> changeFolder(wrapper),
        <.td(),
        <.td(
          <.i(FileTypes.Styles.IconLg(FileTypes.Folder).compose(FolderContentStyle.folder(false))),
          <.span(s" ${wrapper.filename}")
        ),
        <.td(^.className := "text-center", "-"),
        <.td(^.className := "text-center", "-"),
        <.td(^.className := "text-center", "-")
      )

    def render(p: Props) =
      <.div(FolderContentStyle.contentPanel,
        <.div(FolderContentStyle.contentPanelBody,
          <.table(^.className := "table table-striped",
            <.thead(
              <.tr(
                <.th(""),
                <.th("Name"),
                <.th(^.className := "text-center", "Date Added"),
                <.th(^.className := "text-center", "Size"),
                <.th(^.className := "text-center", "Version")
              )
            ),
            <.tbody(
              renderContent(p)
            )
          )
        )
      )

  }

  val component = ReactComponentB[Props]("TableView")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .build

  def apply(
    files: Seq[ManagedFile],
    selectedFolder: ExternalVar[Option[FileId]],
    selectedFile: ExternalVar[Option[ManagedFile]],
    filterText: String = "",
    ctl: RouterCtl[FolderURIElem]
  ) = component(Props(files, selectedFolder, selectedFile, filterText, ctl))

}
