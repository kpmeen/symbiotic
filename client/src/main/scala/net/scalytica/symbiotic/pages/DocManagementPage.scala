package net.scalytica.symbiotic.pages

import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros._
import net.scalytica.symbiotic.components.dman.FileInfo
import net.scalytica.symbiotic.components.dman.foldercontent.FolderContent
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman.{FTree, FolderItem, ManagedFile}
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val fileInfoWrapper = style("tree-col")(
      addClassName("col-md-3"),
      overflow scroll
    )

    val content = style("tree-content-col")(
      addClassNames("col-md-9")
    )
  }

  @Lenses
  case class Props(
    selectedFolder: Option[FileId],
    selectedFile: Option[ManagedFile],
    ctl: RouterCtl[FolderURIElem],
    ftree: FTree,
    status: AjaxStatus,
    reload: Callback
  )

  class Backend($: BackendScope[Props, Props]) {
    def loadFTree(): Callback = $.props.map { p =>
      FTree.load.map {
        case Right(res) => $.modState(_.copy(ftree = res, status = Finished))
        case Left(failed) => $.modState(_.copy(status = failed))
      }.recover {
        case err =>
          log.error(err)
          $.modState(_.copy(status = Failed(err.getMessage)))
      }.map(_.runNow())
    }.void
  }

  val component = ReactComponentB[Props]("DocumentManagement")
    .initialState_P(p => p)
    .backend(new Backend(_))
    .render { $ =>
      // Set up a couple of ExternalVar's to keep track of changes in other components.
      val extSelectedFile = ExternalVar.state($.zoomL(Props.selectedFile))
      val extSelectedFolder = ExternalVar.state($.zoomL(Props.selectedFolder))
      val extFTree = ExternalVar.state($.zoomL(Props.ftree))

      <.div(^.className := "container-fluid")(
        <.div(^.className := "row")(
          <.div(Style.content)(
            FolderContent(extSelectedFolder, extFTree, $.backend.loadFTree(), Nil, extSelectedFile, $.props.ctl)
          ),
          <.div(Style.fileInfoWrapper)(
            FileInfo(extSelectedFile)
          )
        )
      )
    }
    .componentDidMount($ => Callback.when($.isMounted())($.backend.loadFTree()))
    .build

  def apply(p: Props): ReactComponentU[Props, Props, Backend, TopNode] = component(p)

  def apply(
    selectedFolder: Option[FileId],
    selectedFile: Option[ManagedFile],
    ctl: RouterCtl[FolderURIElem]
  ): ReactComponentU[Props, Props, Backend, TopNode] =
    component(Props(selectedFolder, selectedFile, ctl, FTree(FolderItem.empty), Loading, Callback.empty))
}
