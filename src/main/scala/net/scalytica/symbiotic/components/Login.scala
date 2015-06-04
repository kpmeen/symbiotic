package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.HTMLInputElement

import scalaz.effect.ST


object Login {
    case class User(name: String, pass: String)

    class Backend($: BackendScope[_,_]) {
      def handleAdd(user: User): Unit = {
        // TODO validate user
        // TODO redirect to home
        println("Logging in with " + user.name)
      }
    }

    def onNameChange(e: ReactEventI): Unit = {
      println("Hei")
      dummyUser = dummyUser.copy(name = e.target.value)
    }

    def onPassChange(e: ReactEventI): Unit = {
      dummyUser = dummyUser.copy(pass = e.target.value)
    }

    def doLogin(e: SyntheticEvent[HTMLInputElement]): Unit = {
      e.preventDefault()
      println(e.target)
    }

    var dummyUser = User(name = "user", pass = "pass")

    val component = ReactComponentB[Unit]("LoginDialog")
      .initialState(dummyUser)
      .backend(new Backend(_))
      .render(($,S,B) => {
      <.form(
        ^.onSubmit ==> doLogin,
        <.label("Username: "),
        <.input(
          ^.`type` := "text",
          ^.value  := S.name,
          ^.onChange ==> onNameChange
        ),
        <.br,
        <.label("Password: "),
        <.input(
          ^.`type` := "text",
          ^.value  := S.pass,
          ^.onChange ==> onPassChange
        ),
        <.br,
        <.input(
          ^.`type` := "submit",
          ^.value  := "Login"
        )
      )
    })
    .buildU

    def apply() = component()
}
