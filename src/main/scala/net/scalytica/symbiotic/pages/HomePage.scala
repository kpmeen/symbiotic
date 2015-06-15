package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object HomePage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val content = style(
//      textAlign.center,
//      fontSize(30.px),
//      height(100.%%),
//      paddingTop(40.px)
    )
  }

  val component = ReactComponentB.static("HomePage",
    <.div(Style.content, "Symbiotic ")
  ).buildU

  def apply() = component()
}
