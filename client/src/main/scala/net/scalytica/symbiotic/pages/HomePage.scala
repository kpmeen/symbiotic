package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object HomePage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val card = style(
      addClassNames("panel", "panel-default"),
      height(200 px),
      width(300 px)
    )
  }

  val component = ReactComponentB[Unit]("HomePage").render(_ =>
    <.div(^.className := "container-fluid",
      <.div(Style.card,
        <.div(^.className := "panel-heading",
          <.h3(^.className := "panel-title", "Home")
        ),
        <.div(^.className := "panel-body", "Welcome to Symbiotic")
      )
    )
  ).build

  def apply() = component()
}
