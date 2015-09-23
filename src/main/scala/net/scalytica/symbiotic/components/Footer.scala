package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object Footer {

  object Style extends StyleSheet.Inline {

    import dsl._

    val footer = style("footer-style")(
      addClassNames("container-fluid", "text-right"),
      position.absolute,
      bottom.`0`,
      width(100.%%),
      height(60.px),
      backgroundColor.rgb(245,245,245)
    )

  }

  val component = ReactComponentB.static("Footer",
    <.footer(Style.footer, ^.role := "contentinfo",
      <.div(^.className := "container",
        <.p(^.className := "text-muted", "© ... all rights reserved.")
      )
    )
  ).buildU

  def apply() = component()
}