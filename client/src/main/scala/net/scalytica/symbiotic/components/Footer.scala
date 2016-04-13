package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object Footer {

  object Style extends StyleSheet.Inline {

    import dsl._

    val footer = style("footer")(
      addClassNames("container-fluid", "text-right"),
      height(60.px)
    )
    val footerContainer = style("footer-container")(addClassName("container"))
    val footerText = style("footer-text")(addClassName("text-muted"))
  }

  val component = ReactComponentB.static("Footer",
    <.footer(Style.footer, ^.role := "contentinfo",
      <.div(Style.footerContainer,
        <.p(Style.footerText, "©Scalytica.net, all rights reserved.")
      )
    )
  ).build

  def apply() = component()
}