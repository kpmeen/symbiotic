package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.authentication.{LoginForm, RegistrationForm}
import net.scalytica.symbiotic.css.{FontAwesome, LoginStyle}
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.party.User
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.routing.SymbioticRouter._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object LoginPage {

  case class Props(ctl: RouterCtl[View])

  case class State(ctl: RouterCtl[View], register: Boolean = false)

  class Backend($: BackendScope[Props, State]) {

    def socialAuth(provider: String): Callback =
      $.state.map { s =>
        Callback.future(
          User.authenticate(provider).map { success =>
            if (success) s.ctl.set(SymbioticRouter.Home)
            else {
              log.error(s"Unable to authenticate with $provider")
              Callback.alert(s"There was an error trying to authenticate with $provider")
            }
          }
        ).runNow()
      }
  }

  val component = ReactComponentB[Props]("LoginPage")
    .initialState_P(p => State(ctl = p.ctl))
    .backend(new Backend(_))
    .render { $ =>
      <.div(LoginStyle.loginWrapper,
        <.div(LoginStyle.cardWrapper,
          if (!$.state.register) {
            LoginForm($.props.ctl)
          } else {
            RegistrationForm($.props.ctl)
          },

          <.div(LoginStyle.signupCard,
            <.div(^.float.left,
              <.a(LoginStyle.btnGoogle, ^.href := s"$ServerBaseURI/authenticate/google",
                <.i(FontAwesome.google)
              ),
              <.span("\u00a0\u00a0"),
              <.a(LoginStyle.btnTwitter, ^.onClick --> Callback.alert("not yet implemented"), //s"$ServerBaseURI/authenticate/github",
                <.i(FontAwesome.twitter)
              ),
              <.span("\u00a0\u00a0"),
              <.a(LoginStyle.btnGithub, ^.href := s"$ServerBaseURI/authenticate/github",
                <.i(FontAwesome.github)
              )
            ),
            <.div(^.float.right,
              <.a(
                LoginStyle.btnRegister,
                ^.onClick --> $.modState(st => st.copy(register = !st.register)),
                if ($.state.register)
                  <.span(
                    <.i(LoginStyle.toRegister),
                    "\u00a0Login"
                  )
                else
                  <.span(
                    "Register\u00a0",
                    <.i(LoginStyle.toLogin)
                  )
              )
            )
          )
        )
      )
    }
    .build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) =
    component(Props(ctl = ctl))
}