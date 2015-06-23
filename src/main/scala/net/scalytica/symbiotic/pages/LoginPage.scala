package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.User
import net.scalytica.symbiotic.routes.SymbioticRouter
import net.scalytica.symbiotic.routes.SymbioticRouter.View
import net.scalytica.symbiotic.util.Cookies
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.HTMLInputElement

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
      addClassNames("card", "bg-white", "z-depth-5"),
      position.absolute.important,
      width(400.px),
      top(50.%%),
      left(50.%%),
      transform := "translate(-50%, -50%)"
    )
  }

  case class Props(usr: User, ctl: RouterCtl[View])

  class Backend(t: BackendScope[Props, Props]) {
    def onNameChange(e: ReactEventI): Unit = {
      t.modState(s => s.copy(usr = s.usr.copy(name = e.currentTarget.value)))
    }

    def onPassChange(e: ReactEventI): Unit = {
      t.modState(s => s.copy(usr = s.usr.copy(pass = e.currentTarget.value)))
    }

    def doLogin(e: SyntheticEvent[HTMLInputElement]): Unit = {
      val uname = t.state.usr.name
      val passw = t.state.usr.pass
      log.debug(s"username=$uname  password=$passw")

      for {
        res <- Ajax.post(
          url = s"${SymbioticRouter.ServerBaseURI}/login",
          headers = Map(
            "Accept" -> "application/json",
            "Content-Type" -> "application/json"
          ),
          data = s"""{ "username": "$uname", "password": "$passw" }"""
        )
      } yield {
        // TODO: Validate response and potentially redirect to some page
        if (res.status == 200) {
          log.info(s"Success ${res.status}")
          Cookies.set(User.sessionKey, Map("user" -> t.state.usr.name))
          t.state.ctl.set(SymbioticRouter.Home(SymbioticRouter.TestCID)).unsafePerformIO()
        } else {
          log.error(s"Not correct ${res.status}")
        }
      }
    }
  }

  val component = ReactComponentB[Props]("LoginPage")
    .initialStateP(p => p)
    .backend(b => new Backend(b))
    .render((_, props, backend) => {
    <.div(Style.loginWrapper,
      <.div(Style.loginCard,
        <.div(^.className := "card-content",
          <.span(^.className := "card-title grey-text text-darken-4", "Symbiotic Login"),
          <.div(^.className := "row",
            <.div(^.className := "input-field col s12",
              <.input(
                ^.id := "loginUsername",
                ^.className := "validate",
                ^.`type` := "text",
                ^.value := props.usr.name,
                ^.onChange ==> backend.onNameChange
              ),
              <.label(^.`for` := "loginUsername", "Username")
            )
          ),
          <.div(^.className := "row",
            <.div(^.className := "input-field col s12",
              <.input(
                ^.id := "loginPassword",
                ^.className := "validate",
                ^.`type` := "password",
                ^.value := props.usr.pass,
                ^.onChange ==> backend.onPassChange
              ),
              <.label(^.`for` := "loginPassword", "Password")
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
  }).build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) = component(Props(User(name = "", pass = ""), ctl))
}