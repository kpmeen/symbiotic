package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner._
import net.scalytica.symbiotic.css.Material

import scalacss.ScalaCssReact._

object HomePage {

  val component = ReactComponentB.static("HomePage",
    <.div(Material.container,
      <.div(Material.cardMedium,
        <.div(Material.cardContent,
          Spinner(Small),
          <.span(Material.cardTitle, "Symbiotic Home")
        )
      )
    )
  ).buildU

  def apply() = component()
}
