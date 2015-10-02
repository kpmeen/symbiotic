package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.{UserId, Credentials, User}
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.routing.SymbioticRouter.View
import upickle._

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

  class Backend(t: BackendScope[Props, Props]) {
    def onNameChange(e: ReactEventI): Unit =
      t.modState(s => s.copy(creds = s.creds.copy(uname = e.currentTarget.value)))

    def onPassChange(e: ReactEventI): Unit =
      t.modState(s => s.copy(creds = s.creds.copy(pass = e.currentTarget.value)))

    def onKeyEnter(e: ReactKeyboardEventI): Unit = if (e.key == "Enter") doLogin(e)

    def doLogin(e: ReactEventI): Unit = {
      User.login(t.state.creds).map(xhr =>
        if (xhr.status == 200) {
          val uid = read[UserId](xhr.responseText)
          Session.init(t.state.creds.uname, uid.value)
          t.state.ctl.set(SymbioticRouter.Home(SymbioticRouter.TestOrgId)).unsafePerformIO()
        } else {
          throw new Exception(s"Status ${xhr.status}: ${xhr.statusText}")
        }
      ).recover {
        case ex: Throwable =>
          log.error(ex)
          t.modState(_.copy(invalid = true))
      }
    }
  }

  lazy val InvalidCredentials = "Invalid username or password"

  val component = ReactComponentB[Props]("LoginPage")
    .initialStateP(p => p)
    .backend(b => new Backend(b))
    .render { (state, props, backend) =>
      <.div(Style.loginWrapper,
        <.div(Style.loginCard,
          if (props.invalid) {
            <.div(^.className := "alert alert-danger", ^.role := "alert", InvalidCredentials)
          } else {
            ""
          },
          <.form(^.onKeyPress ==> backend.onKeyEnter,
            <.div(^.className := "form-group",
              <.label(^.`for` := "loginUsername", "Username"),
              <.input(
                ^.id := "loginUsername",
                ^.className := "form-control",
                ^.`type` := "text",
                ^.value := props.creds.uname,
                ^.onChange ==> backend.onNameChange
              )
            ),
            <.div(^.className := "form-group",
              <.label(^.`for` := "loginPassword", "Password"),
              <.input(
                ^.id := "loginPassword",
                ^.className := "form-control",
                ^.`type` := "password",
                ^.value := props.creds.pass,
                ^.onChange ==> backend.onPassChange
              )
            )
          ),
          <.div(^.className := "card-action no-border text-right",
            <.input(
              ^.className := "btn btn-primary",
              ^.`type` := "button",
              ^.value := "Login",
              ^.onClick ==> backend.doLogin
            )
          )
        )
      )
    }.build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) =
    component(Props(creds = Credentials("", ""), invalid = false, ctl))
}