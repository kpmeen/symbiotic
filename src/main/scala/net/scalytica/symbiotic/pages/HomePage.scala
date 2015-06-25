package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.{Big, Medium, Small}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object HomePage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style(addClassNames("container"))
    val card = style(addClassNames("card", "medium"))
    val cardContent = style(addClassNames("card-content"))
    val cardTitle = style(addClassNames("card-title", "grey-text", "text-darken-4"))
  }

  val component = ReactComponentB.static("HomePage",
    <.div(Style.container,
      <.div(Style.card,
        <.div(Style.cardContent,
          <.span(Style.cardTitle, "Symbiotic Home"),
          <.div(
            Spinner(Big),
            Spinner(Medium),
            Spinner(Small)
          )
        )
      )
    )
  ).buildU

  def apply() = component()
}
