/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.logger.log

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object RegistrationPage {


  object Style extends StyleSheet.Inline {

    import dsl._

  }

  case class Props()

  class Backend($: BackendScope[Props, Props]) {

    def render(p: Props, s: Props) = {
      <.div()
    }

  }

  val component = ReactComponentB[Props]("RegistrationPage")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .build
}
