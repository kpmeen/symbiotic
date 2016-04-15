package net.scalytica.symbiotic.css

import scalacss.Defaults._

object GlobalStyle extends StyleSheet.Inline {

  import dsl._

  val body = style(unsafeRoot("body")(
    height(100 %%),
    fontSize(14 px),
    fontFamily :=! "Roboto, sans-serif"
  ))

  val symbioticApp = style("symbiotic")(
    height(100 %%)
  )

  val appContent = style(className = "app-content")(
    addClassName("container-fluid"),
    minHeight(100 vh),
    width(100 %%),
    paddingLeft(0 px).important,
    paddingRight(0 px).important
  )

  val main = style("main")(
    addClassName("container-fluid"),
    height(100 %%),
    paddingBottom(60 px),
    overflowY.auto
  )

  // Main menu stuff
  val menuIconSize = mixin(fontSize(18 px))

  val home = style("menu-home")(
    FontAwesome.home,
    menuIconSize
  )

  val library = style("menu-documents")(
    FontAwesome.book,
    menuIconSize
  )

  val profile = style("menu-profile")(
    FontAwesome.user,
    menuIconSize
  )

  val logout = style("menu-logout")(
    FontAwesome.powerOff,
    menuIconSize
  )

  val ulStyle = styleF.bool(isRoot => styleS(
    cursor pointer,
    mixinIfElse(isRoot)(paddingLeft.`0`.important)(paddingLeft(20 px).important)
  ))
}
