/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.pages

import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{Callback, ReactComponentB}
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.routing.SymbioticRouter.{SocialAuthCallback, View}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object AuthCallbackPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val loading = style("authcallback-loading")(
      addClassNames("center-block", "text-center"),
      height(100.%%),
      width(100.%%)
    )
  }

  case class Props(acb: SocialAuthCallback, ctl: RouterCtl[View])

  def validate(p: Props): Callback = {
    println(s"entering ${this.getClass}.Backend.validate")
    println("Going to try authenticating")
    Session.authCodeReceived(p.acb, p.ctl)
  }

  val component = ReactComponentB[Props]("AuthCallbackPage")
    .stateless
    .render_P(p =>
      <.div(Style.loading,
        Spinner(Spinner.Big)
      )
    )
    .componentWillMount(scope => validate(scope.props))
    .build

  def apply(acb: SocialAuthCallback, ctl: RouterCtl[View]) = {
    println(s"Creating ${AuthCallbackPage.getClass}")
    component(Props(acb, ctl))
  }
}
