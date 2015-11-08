/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.Small
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.css.GlobalStyle
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routing.DMan.FolderPath

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
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
      paddingLeft.`0`,
      paddingRight.`0`,
      overflowX.auto
    )

    val treeCardBody = style("tree-body")(
      addClassName("panel-body")
    )

  }

  case class Props(
    oid: String,
    pid: String,
    selectedFolder: Option[String],
    selectedFile: ExternalVar[Option[ManagedFile]],
    status: AjaxStatus,
    ctl: RouterCtl[FolderPath])

  case class State(ftree: FTree, selectedFolder: Option[String], selectedFile: ExternalVar[Option[ManagedFile]], status: AjaxStatus)

  class Backend($: BackendScope[Props, State]) {

    def init(): Callback = $.props.map { p =>
      val x = FTree.load(p.oid).map {
        case Right(res) => $.modState(_.copy(ftree = res, status = Finished))
        case Left(failed) => $.modState(_.copy(status = failed))
      }.recover {
        case err =>
          log.error(err)
          $.modState(_.copy(status = Failed(err.getMessage)))
      }.map(_.runNow())
    }

    def render(p: Props, s: State) = {
      s.status match {
        case Loading =>
          <.div(Style.treeContainer, ^.visibility.hidden,
            <.div(Style.treeCard,
              <.div(Style.treeCardBody,
                <.div(Spinner(Small))
              )
            )
          )
        case Finished =>
          <.div(Style.treeContainer,
            <.div(Style.treeCard,
              <.div(Style.treeCardBody,
                if (s.ftree.folders.nonEmpty) {
                  <.ul(GlobalStyle.ulStyle(true), ^.listStyle := "none",
                    s.ftree.folders.map(fitem =>
                      FolderTreeItem(fitem, s.selectedFolder, s.selectedFile, p.ctl)
                    )
                  )
                } else {
                  <.span("Folder is empty")
                }
              )
            )
          )
        case Failed(err) => <.div(err)
      }
    }
  }

  val component = ReactComponentB[Props]("FolderTree")
    .initialState_P(p => State(FTree(Seq.empty), p.selectedFolder, p.selectedFile, p.status))
    .renderBackend[Backend]
    .componentWillMount(_.backend.init())
    .build

  def apply(props: Props) = component(props)

  def apply(oid: String, pid: String, sfolder: Option[String], sfile: ExternalVar[Option[ManagedFile]], ctl: RouterCtl[FolderPath]) =
    component(Props(oid, pid, sfolder, sfile, Loading, ctl))
}