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
    fontSize(14.px),
    fontFamily := "Roboto, sans-serif"
  ))

  val appContent = style(className = "app-content")(
    display.flex,
    minHeight(100.vh),
    flexDirection.column
  )

  val main = style(unsafeRoot("main")(
    flex := "1",
    display.block
  ))
}
