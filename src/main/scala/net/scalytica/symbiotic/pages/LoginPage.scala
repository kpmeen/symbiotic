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

    val content = style(textAlign.center,
      fontSize(30.px),
      minHeight(650.px),
      paddingTop(40.px))
  }

  case class Props(ctl: RouterCtl[View])

  val component = ReactComponentB[Props]("LoginPage")
    .render(props => {
    <.div(Style.content,
      <.div(Login(props.ctl))
    )
  })
    .build

  def apply(props: Props) = component(props)

  def apply(ctl: RouterCtl[View]) = component(Props(ctl))

}
