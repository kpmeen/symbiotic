package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.Colors
import net.scalytica.symbiotic.routes.Item

import scala.scalajs.js.{Any, UndefOr}
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object LeftNav {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style(
      display.flex,
      flexDirection.column,
      listStyle := "none",
      padding.`0`,
      height(100.%%)
    )

    val menuItem = styleF.bool(selected => styleS(
      lineHeight(48.px),
      padding :=! "0 25px",
      cursor.pointer,
      textDecoration := "none",
      mixinIfElse(selected)(
        Colors.LeftMenuItemSelected,
        color.white,
        fontWeight._500
      )(&.hover(color.black, Colors.LeftMenuItemHover))
    ))
  }

  case class Props(menus: Vector[Item], selectedPage: Item, ctrl: RouterCtl[Item])

  implicit val currentPageReuse = Reusability.by_==[Item]
  implicit val propsReuse = Reusability.by((_: Props).selectedPage)

  val component = ReactComponentB[Props]("LeftNav")
    .render(P => {
    <.ul(Style.container)(
      P.menus.map(item => <.li(^.key := item.title,
        Style.menuItem(item == P.selectedPage),
        item.title,
        P.ctrl setOnClick item))
    )
  })
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(props: Props, ref: UndefOr[String] = "", key: Any = {}) = component.set(key, ref)(props)

}
