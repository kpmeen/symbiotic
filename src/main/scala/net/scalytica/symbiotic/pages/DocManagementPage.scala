package net.scalytica.symbiotic.pages

import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros._
import net.scalytica.symbiotic.components.dman.{FolderTree, FileInfo, FolderContent}
import net.scalytica.symbiotic.models.dman.File
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val tree = style("tree-col")(
      addClassName("col-md-3"),
      overflow.scroll
    )

    val content = style("tree-content-col")(
      addClassNames("col-md-6")
    )

    val preview = style("preview-col")(
      addClassNames("col-md-3")
    )
  }

  @Lenses
  case class Props(
    orgId: String,
    projectId: String,
    selectedFolder: Option[String],
    selectedFile: Option[File],
    ctl: RouterCtl[FolderPath])

  val component = ReactComponentB[Props]("DocumentManagement")
    .initialStateP(p => p)
    .render { $ =>
    val sf = ExternalVar.state($.zoomL(Props.selectedFile))
    <.div(^.className := "container-fluid",
      <.div(^.className := "row",
        <.div(Style.tree,
          FolderTree($.props.orgId, $.props.projectId, $.props.selectedFolder, sf, $.props.ctl)
        ),
        <.div(Style.content, FolderContent($.props.orgId, $.props.selectedFolder, Nil, sf, $.props.ctl)),
        <.div(Style.preview, FileInfo(sf))
      )
    )
  }.build

  def apply(p: Props): ReactComponentU[Props, Props, Unit, TopNode] = component(p)

  def apply(oid: String, pid: String, ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(oid, pid, None, None, ctl))

  def apply(oid: String, pid: String, sf: Option[String], ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(oid, pid, sf, None, ctl))
}
