package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.GlobalStyle.logout
import net.scalytica.symbiotic.models.{Menu, User}
import net.scalytica.symbiotic.routing.SymbioticRouter.View

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object TopNav {

  object Style extends StyleSheet.Inline {

    import dsl._

    val header = style("header")(
      addClassName("container-fluid"),
      paddingLeft.`0`,
      paddingRight.`0`
    )

    val navMenu = style(
      addClassNames("nav", "navbar-nav")
    )

    val navbarRight = style("navright")(
      addClassNames("nav", "navbar-nav", "navbar-right"),
      marginRight(15.px)
    )

    val menuItem = styleF.bool(selected => styleS(
      cursor.pointer,
      mixinIf(selected)(
        addClassName("active")
      )
    ))

  }

  class Backend($: BackendScope[Props, Props]) {
    def doLogout(e: ReactEventI): Callback = $.props.map(p => User.logout(p.ctl))

    def render(p: Props) = {
      <.header(Style.header,
        <.div(^.className := "navbar navbar-default",
          <.div(^.className := "container-fluid",
            <.div(^.className := "navbar-header",
              <.a(^.className := "navbar-brand", ^.href := "/symbiotic#/home", "Symbiotic")
            ),
            <.div(
              <.ul(Style.navMenu,
                p.menus.map(item =>
                  <.li(Style.menuItem(item.route.getClass == p.selectedPage.getClass),
                    item.tag.map { t =>
                      <.a(^.title := item.name, p.ctl setOnClick item.route,
                        t,
                        s" ${item.name}"
                      )
                    }.getOrElse {
                      <.a(item.name, p.ctl setOnClick item.route)
                    }

                  )
                )
              ),
              <.ul(Style.navbarRight,
                <.li(Style.menuItem(false),
                  <.a(^.onClick ==> doLogout, <.i(logout))
                )
              )
            )
          )
        )
      )
    }
  }

  case class Props(menus: Vector[Menu], selectedPage: View, ctl: RouterCtl[View])

  implicit val currentPageReuse = Reusability.by_==[View]
  implicit val propsReuse = Reusability.by((_: Props).selectedPage)

  val component = ReactComponentB[Props]("TopNav")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(props: Props, ref: js.UndefOr[String] = "", key: js.Any = {}) = component.set(key, ref)(props)

}


