package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.User
import net.scalytica.symbiotic.routes.SymbioticRouter
import net.scalytica.symbiotic.routes.SymbioticRouter.View
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.URIUtils

object Login {

  case class Props(usr: User, ctl: RouterCtl[View])

  class Backend(t: BackendScope[Props, Props]) {
    def onNameChange(e: ReactEventI): Unit = {
      t.modState(s => s.copy(usr = s.usr.copy(name = e.currentTarget.value)))
    }

    def onPassChange(e: ReactEventI): Unit = {
      t.modState(s => s.copy(usr = s.usr.copy(pass = e.currentTarget.value)))
    }

    def doLogin(e: SyntheticEvent[HTMLInputElement]): Unit = {
      e.preventDefault()
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
          val cookieValue = s"user=${t.state.usr.name}; path=/"
          dom.document.cookie = s"SYMBIOTIC_USER=${URIUtils.encodeURI(cookieValue)}"
          t.state.ctl.set(SymbioticRouter.Home).unsafePerformIO()
        } else {
          log.error(s"Not correct ${res.status}")
        }
      }
    }
  }

  val component = ReactComponentB[Props]("LoginDialog")
    .initialStateP(p => p)
    .backend(b => new Backend(b))
    .render((_, props, backend) => {
    <.form(
      ^.onSubmit ==> backend.doLogin,
      <.label("Username: "),
      <.input(
        ^.`type` := "text",
        ^.value := props.usr.name,
        ^.onChange ==> backend.onNameChange
      ),
      <.br,
      <.label("Password: "),
      <.input(
        ^.`type` := "text",
        ^.value := props.usr.pass,
        ^.onChange ==> backend.onPassChange
      ),
      <.br,
      <.input(
        ^.`type` := "submit",
        ^.value := "Login"
      )
    )
  }).build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) = component(Props(User(name = "", pass = ""), ctl))
}
