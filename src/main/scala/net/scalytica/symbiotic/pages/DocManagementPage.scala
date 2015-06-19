package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.DocBrowser

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DocManagementPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style(
      display.flex,
      height(100.%%)
    )

    val nav = style(
      width(190.px),
      height(100.%%),
      borderRight :=! "1px solid rgb(223, 220, 220)"
    )

    val content = style(
      padding(30.px)
    )
  }

  val component = ReactComponentB.static("DocumentManagement",
    <.div(Style.container,
      <.div(Style.nav,
        DocBrowser("6a02be50-3dee-42f1-84fb-fbca1b9c4d70")
      ),
      <.div(Style.content, "Hello")
    )
  ).buildU

  def apply() = component()
}
