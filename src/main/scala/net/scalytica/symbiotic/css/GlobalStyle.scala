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
    paddingLeft(0.px).important,
    paddingRight(0.px).important
  )

  val main = style("main")(addClassName("container-fluid"))

  // Main menu stuff
  val menuIconSize = mixin(fontSize(18.px))

  val home = style("menu-home")(
    addClassNames("fa", "fa-home"),
    menuIconSize
  )

  val library = style("menu-documents")(
    addClassNames("fa", "fa-book"),
    menuIconSize
  )

  val profile = style("menu-profile")(
    addClassNames("fa", "fa-user"),
    menuIconSize
  )

  val logout = style("menu-logout")(
    addClassNames("fa", "fa-power-off"),
    menuIconSize
  )
}
