package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.Material
import net.scalytica.symbiotic.routes.SymbioticRouter.{ServerBaseURI, TestOrgId}

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

  val component = ReactComponentB[Unit]("HomePage").render( _ =>
    <.div(Style.container,
      <.div(Style.homeCard,
        <.div(Material.cardContent,
          <.span(Material.cardTitle, "Symbiotic Home")
        )
      )
    )
  ).buildU

  def apply() = component()
}
