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
import net.scalytica.symbiotic.css.Material
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
      paddingLeft(20.px),
      cursor.pointer
    )

    val loading = style(
      Material.valignWrapper,
      height(100.%%),
      width(100.%%)
    )

    val treeContainer = Material.container.compose(style(
      height(100.%%),
      width(100.%%)
    ))

    val treeCard = Material.cardDefault.compose(style(height(100.%%)))

  }

  case class Props(
    cid: String,
    pid: String,
    selectedFolder: Option[String],
    selectedFile: ExternalVar[Option[FileWrapper]],
    status: AjaxStatus,
    ctl: RouterCtl[FolderPath])

  case class State(ftree: FTree, selectedFolder: Option[String], selectedFile: ExternalVar[Option[FileWrapper]], status: AjaxStatus)

  class Backend(t: BackendScope[Props, State]) {

    def init(): Unit = {
      FTree.loadF(t.props.cid).onComplete {
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
              <.div(Material.cardContent,
                <.div(Style.loading, Spinner(Small))
              )
            )
          )
        case Finished =>
          <.div(Style.treeContainer,
            <.div(Style.treeCard,
              if (s.ftree.folders.nonEmpty) {
                <.ul(Style.ulStyle,
                  s.ftree.folders.map(fitem => FolderTreeItem(fitem, s.selectedFolder, s.selectedFile, p.ctl))
                )
              } else {
                <.span("Folder is empty")
              }
            )
          )
        case Failed(err) => <.div(err)
      }
    }.componentWillMount(_.backend.init()).build

  def apply(props: Props) = component(props)

  def apply(cid: String, pid: String, sfolder: Option[String], sfile: ExternalVar[Option[FileWrapper]], ctl: RouterCtl[FolderPath]) =
    component(Props(cid, pid, sfolder, sfile, Loading, ctl))
}