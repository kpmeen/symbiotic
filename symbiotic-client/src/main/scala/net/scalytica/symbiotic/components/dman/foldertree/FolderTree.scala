package net.scalytica.symbiotic.components.dman.foldertree

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.Small
import net.scalytica.symbiotic.core.http.{
  AjaxStatus,
  Failed,
  Finished,
  Loading
}
import net.scalytica.symbiotic.css.GlobalStyle
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderTree {

  object Style extends StyleSheet.Inline {

    import dsl._

    val treeContainer = style("tree-container")(
      addClassName("container-fluid")
    )

    val treeCard = style("tree-card")(
      addClassNames("panel panel-default"),
      paddingLeft `0`,
      paddingRight `0`,
      overflowX auto
    )

    val treeCardBody = style("tree-body")(
      addClassName("panel-body")
    )

  }

  case class Props(
      selectedFolder: ExternalVar[Option[FileId]],
      ftree: ExternalVar[FTree],
      sfile: ExternalVar[Option[ManagedFile]],
      status: AjaxStatus,
      ctl: RouterCtl[FolderURIElem]
  )

  class Backend($ : BackendScope[Props, Unit]) {
    def render(p: Props) = {
      p.status match {
        case Loading =>
          <.div(
            Style.treeContainer,
            ^.visibility.hidden,
            <.div(
              Style.treeCard,
              <.div(Style.treeCardBody, <.div(Spinner(Small)))
            )
          )
        case Finished =>
          <.div(
            Style.treeContainer,
            <.div(
              Style.treeCard,
              <.div(
                Style.treeCardBody,
                p.ftree.value.root match {
                  case FolderItem.empty =>
                    <.span("Folder is empty")

                  case root =>
                    <.ul(
                      GlobalStyle.ulStyle(true),
                      ^.listStyle := "none",
                      root.children.map(
                        fti =>
                          FolderTreeItem(fti, p.selectedFolder, p.sfile, p.ctl)
                      )
                    )
                }
              )
            )
          )
        case Failed(err) => <.div(err)
      }
    }
  }

  val component =
    ReactComponentB[Props]("FolderTree").renderBackend[Backend].build

  def apply(props: Props) = component(props)

  def apply(
      sfolder: ExternalVar[Option[FileId]],
      ftree: ExternalVar[FTree],
      sfile: ExternalVar[Option[ManagedFile]],
      status: AjaxStatus,
      ctl: RouterCtl[FolderURIElem]
  ) = component(Props(sfolder, ftree, sfile, status, ctl))
}
