package net.scalytica.symbiotic.components.dman.foldercontent

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.dman.foldercontent.ContentView_PS._
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object IconView {

  object Style extends StyleSheet.Inline {

    import dsl._

    val fcGrouping = styleF.bool(
      selected =>
        styleS(
          addClassNames("center-block", "text-center"),
          display inlineTable,
          padding(5 px),
          margin(5 px),
          width(120 px),
          height(100 px),
          cursor pointer,
          mixinIfElse(selected)(
            backgroundColor rgb (190, 220, 230)
          )(
            &.hover(backgroundColor rgb (222, 222, 222)),
            textDecoration := "none"
          )
      )
    )

    val folderLabel = style(
      fontSize(14 px),
      color darkslategrey,
      wordWrap breakWord,
      wordBreak breakAll
    )
  }

  class Backend(val $ : BackendScope[Props, Props]) extends ContentViewBackend {

    override def renderFile(
        selected: Option[ManagedFile],
        contentType: FileTypes.FileType,
        wrapper: ManagedFile
    ): ReactElement =
      <.div(
        Style.fcGrouping(selected.contains(wrapper)),
        ^.onClick --> setSelected(wrapper),
        <.i(
          FileTypes.Styles
            .Icon3x(contentType)
            .compose(FolderContentStyle.file(true))
        ),
        <.a(
          ^.id := wrapper.metadata.fid,
          ^.href := wrapper.downloadLink,
          ^.onClick ==> { (e: ReactEvent) =>
            downloadFile(e, wrapper)
          },
          <.span(
            Style.folderLabel,
            wrapper.filename,
            wrapper.metadata.lock.map { l =>
              <.i(^.className := "fa fa-lock", ^.marginLeft := "5px")
            }.getOrElse(EmptyTag)
          )
        )
      )

    override def renderFolder(
        selected: Option[ManagedFile],
        wrapper: ManagedFile
    ): ReactElement =
      <.div(
        Style.fcGrouping(false),
        ^.onClick --> changeFolder(wrapper),
        <.i(
          FileTypes.Styles
            .Icon3x(FileTypes.Folder)
            .compose(FolderContentStyle.folder(true))
        ),
        <.a(Style.folderLabel, wrapper.filename)
      )

    def render(p: Props) =
      <.div(
        FolderContentStyle.contentPanel,
        <.div(
          FolderContentStyle.contentPanelBody,
          <.div(
            ^.className := "container-fluid",
            if (p.files.nonEmpty) renderContent(p)
            else <.span("Folder is empty")
          )
        )
      )
  }

  val component = ReactComponentB[Props]("IconView")
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
