package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Login, LeftNav}
import net.scalytica.symbiotic.routes.Item

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._

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
