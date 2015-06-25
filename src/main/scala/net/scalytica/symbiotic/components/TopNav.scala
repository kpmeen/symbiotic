package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.extra.{LogLifecycle, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.Colors
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.{Menu, User}
import net.scalytica.symbiotic.routes.SymbioticRouter.View
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object TopNav {

  object Style extends StyleSheet.Inline {

    import dsl._

    val navMenu = style(
      Colors.TopMenuColor,
      display.flex,
      alignItems.center,
      margin.`0`,
      listStyle := "none"
    )

    val menuItem = styleF.bool(selected => styleS(
      paddingLeft(20.px),
      paddingRight(20.px),
      fontSize(1.5.em),
      cursor.pointer,
      color(c"rgb(244, 233, 233)"),
      mixinIfElse(selected)(
        Colors.TopMenuItemSelected,
        fontWeight._300
      )(&.hover(
        Colors.TopMenuItemHover
      ))
    ))

  }

  class Backend(t: BackendScope[Props, Props]) {
    def doLogout(e: SyntheticEvent[HTMLInputElement]): Unit = {
      User.logout(t.props.ctl)
    }
  }

  case class Props(menus: Vector[Menu], selectedPage: View, ctl: RouterCtl[View])

  implicit val currentPageReuse = Reusability.by_==[View]
  implicit val propsReuse = Reusability.by((_: Props).selectedPage)

  val component = ReactComponentB[Props]("TopNav")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render((P, S, B) =>
      <.header(
        <.nav(
          <.ul(Style.navMenu,
            P.menus.map(item =>
              <.li(^.key := item.name, Style.menuItem(item.route.getClass == P.selectedPage.getClass), item.name, P.ctl setOnClick item.route)
            ),
            <.li(^.key := "Logout", Style.menuItem(false), "logout", ^.onClick ==> B.doLogout)
          )
        )
      )
    )
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(props: Props, ref: js.UndefOr[String] = "", key: js.Any = {}) = component.set(key, ref)(props)

}


