package net.scalytica.symbiotic.css

import scalacss.Defaults._
import scalacss.{Attr, CanIUse, Transform}

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
    height(100.vh),
    flexDirection.column
  )

  val main = style(unsafeRoot("main")(
    flex := "1",
    display.block
  ))

  val appearance = Attr.real("appearance", Transform keys CanIUse.boxdecorationbreak)

  val scrollbar = style(unsafeRoot("::-webkit-scrollbar")(
    appearance := "none",
    width(7.px)
  ))

  val scrollbarThumb = style(unsafeRoot("::-webkit-scrollbar-thumb")(
    borderRadius(4.px),
    backgroundColor(c"rgba(0,0,0,.5)"),
    boxShadow := "0 0 1px rgba(255,255,255,.5)"
  ))
}
