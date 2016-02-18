package net.scalytica.symbiotic.pages

import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros._
import net.scalytica.symbiotic.components.dman.foldercontent.FolderContent
import net.scalytica.symbiotic.components.dman.foldertree.FolderTree
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.routing.DMan.FolderPath

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
      addClassNames("col-md-9")
    )
  }

  @Lenses
  case class Props(
    selectedFolder: Option[String],
    selectedFile: Option[ManagedFile],
    ctl: RouterCtl[FolderPath]
  )

  val component = ReactComponentB[Props]("DocumentManagement")
    .initialState_P(p => p)
    .render { $ =>
      val sf = ExternalVar.state($.zoomL(Props.selectedFile))
      <.div(^.className := "container-fluid")(
        <.div(^.className := "row")(
          <.div(Style.tree)(
            FolderTree($.props.selectedFolder, sf, $.props.ctl)
          ),
          <.div(Style.content)(
            FolderContent($.props.selectedFolder, Nil, sf, $.props.ctl)
          )
        )
      )
    }.build

  def apply(p: Props): ReactComponentU[Props, Props, Unit, TopNode] = component(p)

  def apply(ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(None, None, ctl))

  def apply(sf: Option[String], ctl: RouterCtl[FolderPath]): ReactComponentU[Props, Props, Unit, TopNode] =
    component(Props(sf, None, ctl))
}
