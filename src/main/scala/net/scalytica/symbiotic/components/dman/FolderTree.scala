/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.Small
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routes.DMan.FolderPath
import net.scalytica.symbiotic.util.{AjaxStatus, Failed, Finished, Loading}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderTree {

  object Style extends StyleSheet.Inline {

    import dsl._

    val ulStyle = style(className = "tree-root")(
      cursor.pointer,
      paddingLeft.`0`
    )

    val loading = style(
//      Material.valignWrapper,
    )

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
    selectedFile: ExternalVar[Option[File]],
    status: AjaxStatus,
    ctl: RouterCtl[FolderPath])

  case class State(ftree: FTree, selectedFolder: Option[String], selectedFile: ExternalVar[Option[File]], status: AjaxStatus)

  class Backend(t: BackendScope[Props, State]) {

    def init(): Unit = {
      FTree.loadF(t.props.oid).onComplete {
        case Success(s) => s match {
          case Right(res) => t.modState(_.copy(ftree = res, status = Finished))
          case Left(failed) => t.modState(_.copy(status = failed))
        }
        case Failure(err) =>
          log.error(err)
          t.modState(_.copy(status = Failed(err.getMessage)))
      }
      t.modState(_.copy(status = Loading))
    }
  }

  val component = ReactComponentB[Props]("FolderTree")
    .initialStateP(p => State(FTree(Seq.empty), p.selectedFolder, p.selectedFile, p.status))
    .backend(new Backend(_))
    .render { (p, s, b) =>
      s.status match {
        case Loading =>
          <.div(Style.treeContainer,
            <.div(Style.treeCard,
              <.div(Style.treeCardBody,
                <.div(Style.loading, Spinner(Small))
              )
            )
          )
        case Finished =>
          <.div(Style.treeContainer,
            <.div(Style.treeCard,
              <.div(Style.treeCardBody,
                if (s.ftree.folders.nonEmpty) {
                  <.ul(Style.ulStyle, ^.listStyle := "none",
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
    }.componentWillMount(_.backend.init()).build

  def apply(props: Props) = component(props)

  def apply(oid: String, pid: String, sfolder: Option[String], sfile: ExternalVar[Option[File]], ctl: RouterCtl[FolderPath]) =
    component(Props(oid, pid, sfolder, sfile, Loading, ctl))
}