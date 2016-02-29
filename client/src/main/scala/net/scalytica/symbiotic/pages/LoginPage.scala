package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
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
      width(100.%%).important
    )

    val cardWrapper = style(
      position.absolute.important,
      transform := "translate(-50%, -50%)",
      width(400.px),
      top(50.%%),
      left(50.%%)
    )

    val card = style(
      addClassNames("panel", "panel-default", "z-depth-5"),
      padding(50.px)
    )
  }

  case class Props(creds: Credentials, invalid: Boolean, ctl: RouterCtl[View])

  class Backend($: BackendScope[Props, Props]) {
    def onNameChange(e: ReactEventI) =
      $.modState(s => s.copy(creds = s.creds.copy(uname = e.target.value)))

    def onPassChange(e: ReactEventI) =
      $.modState(s => s.copy(creds = s.creds.copy(pass = e.target.value)))

    //    def onKeyEnter(e: ReactKeyboardEventI) =
    //      Callback(if (e.key == "Enter") doLogin(e))

    def doLogin(creds: Credentials): Callback = {
      $.state.map { s =>
        Callback.future(
          User.login(creds).map { success =>
            if (success) {
              s.ctl.set(SymbioticRouter.Home)
            }
            else {
              log.error("Unable to authenticate with credentials")
              $.modState(_.copy(invalid = true))
            }
          }
        ).runNow()
      }
    }

    def socialAuth(provider: String): Callback = {
      $.state.map { s =>
        Callback.future(
          User.authenticate(provider).map { success =>
            if (success) {
              s.ctl.set(SymbioticRouter.Home)
            }
            else {
              log.error(s"Unable to authenticate with $provider")
              $.modState(_.copy(invalid = true))
            }
          }
        ).runNow()
      }
    }

    def render(p: Props, s: Props) = {
      <.div(Style.loginWrapper,
        <.div(Style.cardWrapper,
          <.div(Style.card,
            if (p.invalid) {
              <.div(^.className := "alert alert-danger", ^.role := "alert", InvalidCredentials)
            } else {
              ""
            },
            <.form(//^.onKeyPress ==> onKeyEnter,
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
                ^.className := "btn btn-primary",
                ^.`type` := "button",
                ^.value := "Login",
                ^.onClick --> doLogin(s.creds)
              )
            )
          ),
          <.div(Style.card,
//            <.input(
//              ^.className := "btn btn-primary",
//              ^.`type` := "button",
//              ^.value := "Google",
//              ^.onClick --> socialAuth("google")
//            )
            // This works...but not getting any sensible response or possibility to set Session Cookie
            <.a(^.className := "btn btn-danger", ^.href := s"$ServerBaseURI/authenticate/google")("Google"),
            <.input(
              ^.className := "btn btn-primary",
              ^.`type` := "button",
              ^.value := "AUTH REDIRECT",
              ^.onClick --> p.ctl.set(SocialAuthCallback("?foo=bar&code=asdfasdf"))
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