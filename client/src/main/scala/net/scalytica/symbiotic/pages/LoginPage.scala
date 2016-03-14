package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.FontAwesome
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.{Credentials, User}
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.routing.SymbioticRouter._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object LoginPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val loginWrapper = style(
      position.relative.important,
      height(100.%%).important,
      width(100.%%).important,
      backgroundImage := "url('/resources/images/login_background.jpg')",
      backgroundSize := "cover"//"100% 100%"
    )

    val cardWrapper = style(
      position.absolute.important,
      transform := "translate(-50%, -50%)",
      width(400.px),
      top(50.%%),
      left(50.%%)
    )

    val largerButton = mixin(fontSize.large)

    val largerButtonBold = mixin(
      largerButton,
      fontWeight.bolder
    )

    val btnRegister = style("btn-register")(
      largerButton,
      addClassNames("btn", "btn-primary")
    )

    val btnTwitter = style("btn-twitter")(
      largerButtonBold,
      addClassNames("btn", "btn-info"),
      color.white
    )

    val btnGithub = style("btn-github")(
      largerButtonBold,
      addClassName("btn"),
      backgroundColor.grey(80),
      color.white
    )

    val btnGoogle = style("btn-google")(
      largerButtonBold,
      addClassNames("btn", "btn-danger")
    )

    val loginCard = style("login-card")(
      addClassNames("panel", "panel-default", "z-depth-5"),
      padding(50.px),
      backgroundColor.rgba(255, 255, 255, 0.8)
    )

    val nakedCard = style("signup-card")(
      addClassNames("panel", "panel-default", "z-depth-5"),
      padding.`0`,
      backgroundColor.transparent,
      border.`0`
    )
  }

  case class Props(creds: Credentials, invalid: Boolean, ctl: RouterCtl[View])

  class Backend($: BackendScope[Props, Props]) {
    def onNameChange(e: ReactEventI) =
      $.modState(s => s.copy(creds = s.creds.copy(uname = e.target.value)))

    def onPassChange(e: ReactEventI) =
      $.modState(s => s.copy(creds = s.creds.copy(pass = e.target.value)))

    def onKeyEnter(e: ReactKeyboardEventI) =
      if (e.key == "Enter") $.state.flatMap(s => doLogin(s.creds))
      else Callback.empty

    def doLogin(creds: Credentials): Callback =
      $.state.map { s =>
        Callback.future(
          User.login(creds).map { success =>
            if (success) s.ctl.set(SymbioticRouter.Home)
            else {
              log.error("Unable to authenticate with credentials")
              $.modState(_.copy(invalid = true))
            }
          }
        ).runNow()
      }

    def socialAuth(provider: String): Callback =
      $.state.map { s =>
        Callback.future(
          User.authenticate(provider).map { success =>
            if (success) s.ctl.set(SymbioticRouter.Home)
            else {
              log.error(s"Unable to authenticate with $provider")
              $.modState(_.copy(invalid = true))
            }
          }
        ).runNow()
      }

    def render(p: Props, s: Props) = {
      <.div(Style.loginWrapper, ^.onKeyPress ==> onKeyEnter,
        <.div(Style.cardWrapper,
          <.div(Style.loginCard,
            if (p.invalid) {
              <.div(^.className := "alert alert-danger", ^.role := "alert", InvalidCredentials)
            } else {
              ""
            },
            <.form(
              <.div(^.className := "form-group",
                <.label(^.`for` := "loginUsername", "Username"),
                <.input(
                  ^.id := "loginUsername",
                  ^.className := "form-control",
                  ^.`type` := "text",
                  ^.value := s.creds.uname,
                  ^.onChange ==> onNameChange
                )
              ),
              <.div(^.className := "form-group",
                <.label(^.`for` := "loginPassword", "Password"),
                <.input(
                  ^.id := "loginPassword",
                  ^.className := "form-control",
                  ^.`type` := "password",
                  ^.value := s.creds.pass,
                  ^.onChange ==> onPassChange
                )
              )
            ),
            <.div(^.className := "card-action no-border text-right",
              <.input(
                ^.className := "btn btn-success",
                ^.tpe := "button",
                ^.value := "Login",
                ^.onClick --> doLogin(s.creds)
              )
            )
          ),
          <.div(Style.nakedCard,
            <.div(^.float.left,
              <.a(Style.btnGoogle, ^.href := s"$ServerBaseURI/authenticate/google",
                <.i(FontAwesome.google)
              ),
              <.span("\u00a0\u00a0"),
              <.a(Style.btnTwitter, ^.onClick --> Callback.alert("not yet implemented"), //s"$ServerBaseURI/authenticate/github",
                <.i(FontAwesome.twitter)
              ),
              <.span("\u00a0\u00a0"),
              <.a(Style.btnGithub, ^.href := s"$ServerBaseURI/authenticate/github",
                <.i(FontAwesome.github)
              )
            ),
            <.div(^.float.right,
              <.input(
                Style.btnRegister,
                ^.tpe := "button",
                ^.value := "Register",
                ^.onClick --> Callback.alert("Patience, grashopper! This is not yet implemented.")
              )
            )
          )
        )
      )
    }
  }

  lazy val InvalidCredentials = "Invalid username or password"

  val component = ReactComponentB[Props]("LoginPage")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) =
    component(Props(creds = Credentials("", ""), invalid = false, ctl))
}