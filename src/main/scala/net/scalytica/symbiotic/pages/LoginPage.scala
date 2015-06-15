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

    val wrapper = style(className = "mywrapper")(
      position.relative.important,
      height(100.%%).important,
      width(100.%%).important
    )

    val card = style(
      addClassNames("card", "bg-white"),
      position.absolute.important,
      width(400.px),
      top(50.%%),
      left(50.%%),
      transform := "translate(-50%, -50%)"
    )
  }

  case class Props(ctl: RouterCtl[View])

  val component = ReactComponentB[Props]("LoginPage")
    .render(props => {
    <.div(Style.wrapper,
      <.div(Style.card,
        Login(props.ctl)
      )
    )
  }).build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) = component(Props(ctl))

}
