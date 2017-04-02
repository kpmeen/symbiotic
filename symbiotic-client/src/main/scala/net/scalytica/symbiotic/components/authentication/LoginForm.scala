/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.authentication

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.LoginStyle
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.Credentials
import net.scalytica.symbiotic.models.party.User
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.routing.SymbioticRouter.View

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object LoginForm {

  case class Props(ctl: RouterCtl[View], creds: Credentials)

  case class State(ctl: RouterCtl[View], creds: Credentials, error: Boolean = false, register: Boolean = false)

  class Backend($: BackendScope[Props, State]) {
    def onNameChange(e: ReactEventI) = {
      e.persist()
      $.modState(s => s.copy(creds = s.creds.copy(uname = e.target.value)))
    }

    def onPassChange(e: ReactEventI) = {
      e.persist()
      $.modState(s => s.copy(creds = s.creds.copy(pass = e.target.value)))
    }

    def onKeyEnter(e: ReactKeyboardEventI) =
      if (e.key == "Enter") $.state.flatMap(s => doLogin(s.creds))
      else Callback.empty

    def doLogin(creds: Credentials): Callback =
      $.state.map { s =>
        Callback.future(
          User.login(creds).map(success =>
            if (success) s.ctl.set(SymbioticRouter.Home)
            else {
              log.error("Unable to authenticate with credentials")
              $.modState(_.copy(error = true))
            }
          )
        ).runNow()
      }

    def render(props: Props, state: State) = {
      <.div(LoginStyle.loginCard, ^.onKeyPress ==> onKeyEnter,
        if (state.error) {
          <.div(^.className := "alert alert-danger", ^.role := "alert", "Invalid username or password.")
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
              ^.value := state.creds.uname,
              ^.onChange ==> onNameChange
            )
          ),
          <.div(^.className := "form-group",
            <.label(^.`for` := "loginPassword", "Password"),
            <.input(
              ^.id := "loginPassword",
              ^.className := "form-control",
              ^.`type` := "password",
              ^.value := state.creds.pass,
              ^.onChange ==> onPassChange
            )
          )
        ),
        <.div(^.className := "card-action no-border text-right",
          <.input(
            ^.className := "btn btn-success",
            ^.tpe := "button",
            ^.value := "Login",
            ^.onClick --> doLogin(state.creds)
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("LoginForm")
    .initialState_P(p => State(ctl = p.ctl, creds = p.creds))
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) =
    component(Props(ctl = ctl, creds = Credentials("", "")))

}
