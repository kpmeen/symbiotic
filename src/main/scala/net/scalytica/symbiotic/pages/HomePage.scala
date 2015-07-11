package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.dropzone.DropzoneComponent
import net.scalytica.symbiotic.css.Material

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object HomePage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = Material.container.compose(style(
      display.flex,
      height(100.%%),
      width(100.%%)
    ))

    val homeCard = Material.cardDefault.compose(style(
      Material.col,
      addClassName("s12"),
      height(100.%%),
      width(100.%%)
    ))

  }

  val component = ReactComponentB.static("HomePage",
    <.div(Style.container,
      <.div(Style.homeCard,
        <.div(Material.cardContent,
          <.span(Material.cardTitle, "Symbiotic Home"),
          DropzoneComponent()
        )
      )
    )
  ).buildU

  def apply() = component()
}
