package net.scalytica.symbiotic.pages

import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros._
import net.scalytica.symbiotic.components.dman.{FileInfo, FolderContent, FolderTree}
import net.scalytica.symbiotic.css.Material
import net.scalytica.symbiotic.models.dman.File
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = Material.row.compose(style(
      width(100.%%)
    ))

    val tree = style("tree-col")(
      Material.col,
      addClassName("s3"),
      overflow.scroll
    )

    val content = style("tree-content-col")(
      Material.col,
      addClassNames("s7")
    )

    val preview = style("preview-col")(
      Material.col,
      addClassNames("s2")
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
    <.div(Style.container,
      <.div(Style.tree, FolderTree($.props.orgId, $.props.projectId, $.props.selectedFolder, sf, $.props.ctl)),
      <.div(Style.content, FolderContent($.props.orgId, $.props.selectedFolder, Nil, sf, $.props.ctl)),
      <.div(Style.preview, FileInfo(sf))
    )
  }.build

  def apply(p: Props): ReactComponentU[Props, Props, Unit, TopNode] = component(p)

  def apply(oid: String, pid: String, ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(oid, pid, None, None, ctl))

  def apply(oid: String, pid: String, sf: Option[String], ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(oid, pid, sf, None, ctl))
}
