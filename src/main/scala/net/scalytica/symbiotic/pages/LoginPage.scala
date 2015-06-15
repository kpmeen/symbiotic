package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Login
import net.scalytica.symbiotic.routes.SymbioticRouter.View

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object LoginPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val cardWrapper = style(
      marginLeft(50.px),
      marginRight(50.px)
    )
    val card = style(
      addClassNames("card", "bg-white")
    )
  }

  case class Props(ctl: RouterCtl[View])

  val component = ReactComponentB[Props]("LoginPage")
    .render(props => {
    <.div(Style.cardWrapper,
      <.div(Style.card,
        <.div(Login(props.ctl))
      )
    )
  })
    .build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) = component(Props(ctl))

}
