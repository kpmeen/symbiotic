package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Login

object LoginPage {
    val component = ReactComponentB[Unit]("LoginPage")
      .render(_ => {
      <.div(
        <.div(Login())
      )
    })
    .buildU

  def apply() = component()

}
