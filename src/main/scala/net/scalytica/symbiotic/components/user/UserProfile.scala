/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.user

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object UserProfile {

  //  case class Props(usr: User)

  //  val component = ReactComponentB[Props]("UserProfile")
  //    .initialStateP(p => p)
  //    .render { $ =>
  val component = ReactComponentB.static("UserProfile",
    <.div(^.className := "well",
      <.h2("James Bond"),
      <.p(
        <.strong("About:"),
        "My name is Bond, James Bond"
      )
    )
  ).buildU

  def apply() = component()
}
