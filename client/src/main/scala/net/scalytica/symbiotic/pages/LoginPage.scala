package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.{Credentials, User, UserId}
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.routing.SymbioticRouter.View
import upickle.default._

import ScalazReact._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
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

    val loginCard = style(
      addClassNames("panel", "panel-default", "z-depth-5"),
      padding(50.px),
      position.absolute.important,
      transform := "translate(-50%, -50%)",
      width(400.px),
      top(50.%%),
      left(50.%%)
    )
  }

  case class Props(creds: Credentials, invalid: Boolean, ctl: RouterCtl[View])

  class Backend($: BackendScope[Props, Props]) {
    def onNameChange(e: ReactEventI) =
      $.modState(s => s.copy(creds = s.creds.copy(uname = e.target.value)))

    def onPassChange(e: ReactEventI) =
      $.modState(s => s.copy(creds = s.creds.copy(pass = e.target.value)))

    def onKeyEnter(e: ReactKeyboardEventI) =
      Callback(if (e.key == "Enter") doLogin(e))

    def doLogin(e: ReactEventI): Callback = {
      $.state.map(s =>
        User.login(s.creds).map(xhr =>
          if (xhr.status == 200) {
            val uid = read[UserId](xhr.responseText)
            Session.init(s.creds.uname, uid.value)
            s.ctl.set(SymbioticRouter.Home).toIO.unsafePerformIO()
          } else {
            throw new Exception(s"Status ${xhr.status}: ${xhr.statusText}")
          }
        ).recover {
          case ex: Throwable =>
            log.error(ex)
            $.modState(_.copy(invalid = true))
        }
      )
    }

    def render(p: Props, s: Props) = {
      <.div(Style.loginWrapper,
        <.div(Style.loginCard,
          if (p.invalid) {
            <.div(^.className := "alert alert-danger", ^.role := "alert", InvalidCredentials)
          } else {
            ""
          },
          <.form(^.onKeyPress ==> onKeyEnter,
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
              ^.onClick ==> doLogin
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