package net.scalytica.symbiotic.css

import scalacss.Defaults._

object GlobalStyle extends StyleSheet.Inline {

  import dsl._

  val html = style(unsafeRoot("html")(
    height(100.%%)
  ))

  val body = style(unsafeRoot("body")(
    position.absolute,
    top.`0`,
    bottom.`0`,
    right.`0`,
    left.`0`,
    margin.`0`,
    padding.`0`,
    height(100.%%),
    fontSize(14.px),
    fontFamily := "Roboto, sans-serif"
  ))
}
