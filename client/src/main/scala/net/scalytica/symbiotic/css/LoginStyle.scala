/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

import scalacss.Defaults._

object LoginStyle extends StyleSheet.Inline {
  import dsl._

  val loginWrapper = style(
    position.relative.important,
    height(100 %%).important,
    width(100 %%).important,
    backgroundImage := "url('/resources/images/login_background.jpg')",
    backgroundSize := "cover"//"100% 100%"
  )

  val cardWrapper = style(
    position.absolute.important,
    transform := "translate(-50%, -50%)",
    width(400 px),
    top(50 %%),
    left(50 %%)
  )

  val largerButton = mixin(fontSize large)

  val largerButtonBold = mixin(
    largerButton,
    fontWeight bolder
  )

  val btnRegister = style("btn-register")(
    largerButton,
    addClassNames("btn", "btn-primary")
  )

  val btnTwitter = style("btn-twitter")(
    largerButtonBold,
    addClassNames("btn", "btn-info"),
    color white
  )

  val btnGithub = style("btn-github")(
    largerButtonBold,
    addClassName("btn"),
    backgroundColor grey(80),
    color white
  )

  val btnGoogle = style("btn-google")(
    largerButtonBold,
    addClassNames("btn", "btn-danger")
  )

  val loginCard = style("login-card")(
    addClassNames("panel", "panel-default", "z-depth-5"),
    padding(50 px),
    backgroundColor rgba(255, 255, 255, 0.8)
  )

  val signupCard = style("signup-card")(
    addClassNames("panel", "panel-default", "z-depth-5"),
    padding `0`,
    backgroundColor transparent,
    border `0`
  )

  val toRegister = style("to-register")(FontAwesome.chevronLeft)
  val toLogin = style("to-login")(FontAwesome.chevronRight)

  val formElemHasError = styleF.bool(isValid => styleS(
    addClassName("form-group"),
    mixinIf(!isValid)(addClassName("has-error"))
  ))
}