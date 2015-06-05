package net.scalytica.symbiotic.components
import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.extra.router2.{Redirect, RedirectToPage}
import net.scalytica.symbiotic.models.User
import net.scalytica.symbiotic.routes.AppRouter.Home
import org.scalajs.dom
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.HTMLInputElement

object Login {

  class Backend($: BackendScope[User, User]) {
    def onNameChange(e: ReactEventI): Unit = {
      $.modState(_.copy(name = e.currentTarget.value))
    }

    def onPassChange(e: ReactEventI): Unit = {
      $.modState(_.copy(pass = e.currentTarget.value))
    }

    def doLogin(e: SyntheticEvent[HTMLInputElement]): Unit = {
      e.preventDefault()
      // TODO validate user
      dom.document.cookie = "PLAY_SESSION=c45ccf25ce170e59e166a06d27ae54770ac1fecc-userId=paul.pm%40qatar-oil.com&sessionId=557186a4e4b0bd6b70dcaecf&lastAccess=1433503396857; expires=Fri, 31 Dec 9999 23:59:59 GMT; path=/"
      // TODO redirect to home
    }
  }

  val component = ReactComponentB[User]("LoginDialog")
    .initialStateP(p => User(p.name, p.pass))
    .backend(new Backend(_))
    .render((_, S, B) => {
      <.form(
        ^.onSubmit ==> B.doLogin,
        <.label("Username: "),
        <.input(
          ^.`type` := "text",
          ^.value := S.name,
          ^.onChange ==> B.onNameChange
        ),
        <.br,
        <.label("Password: "),
        <.input(
          ^.`type` := "text",
          ^.value := S.pass,
          ^.onChange ==> B.onPassChange
        ),
        <.br,
        <.input(
          ^.`type` := "submit",
          ^.value := "Login"
        )
      )
    }).build

  def apply(props: User) = component(props)
  
  def apply() = component(User(name = "", pass = ""))
}
