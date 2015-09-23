package net.scalytica.symbiotic.css

import scalacss.Defaults._

object GlobalStyle extends StyleSheet.Inline {

  import dsl._

  val body = style(unsafeRoot("body")(
    height(100.vh),
    fontSize(14.px),
    fontFamily := "Roboto, sans-serif"
  ))

  val appContent = style(className = "app-content")(
    addClassName("container-fluid"),
    paddingLeft(5.px).important,
    paddingRight(5.px).important
  )

  val main = style(unsafeRoot("main")(
    addClassName("container-fluid")
  ))

  val head = style(unsafeRoot("header")(
    addClassName("container-fluid")
  ))
}
