package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.HTMLInputElement

object Login {

  case class User(name: String, pass: String)

  class Backend($: BackendScope[User, User]) {
    def handleAdd(user: User): Unit = {
      // TODO validate user
      // TODO redirect to home
      println("Logging in with " + user.name)
    }

    def onNameChange(e: ReactEventI): Unit = {
      println(e.currentTarget.value)
      $.modState(s => s.copy(name = e.currentTarget.value))
    }

    def onPassChange(e: ReactEventI): Unit = {
      println(e.currentTarget.value)
      $.modState(s => s.copy(pass = e.currentTarget.value))
    }

    def doLogin(e: SyntheticEvent[HTMLInputElement]): Unit = {
      e.preventDefault()
      println(e.target.value)
    }
  }

  val component = ReactComponentB[User]("LoginDialog")
    .initialStateP(p => User(p.name, p.pass))
    .backend(new Backend(_))
    .render((P, S, B) => {
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
