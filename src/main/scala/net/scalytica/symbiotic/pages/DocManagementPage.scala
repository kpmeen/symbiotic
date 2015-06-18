package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.DocBrowser

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style(addClassNames("container"))
    val card = style(addClassNames("card", "medium"))
    val cardContent = style(addClassNames("card-content"))
    val cardTitle = style(addClassNames("card-title", "grey-text", "text-darken-4"))
  }

  val component = ReactComponentB.static("DocumentManagement",
    <.div(Style.container,
      DocBrowser("6a02be50-3dee-42f1-84fb-fbca1b9c4d70")
    )
  ).buildU

  def apply() = component()
}
