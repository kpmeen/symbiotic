package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.GlobalStyle.logout
import net.scalytica.symbiotic.models.{Menu, User}
import net.scalytica.symbiotic.routing.SymbioticRouter.View
import org.scalajs.dom.raw.HTMLInputElement

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

    val menuItem = styleF.bool(selected => styleS(
      cursor.pointer,
      mixinIf(selected)(
        addClassName("active")
      )
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
      <.header(Style.header,
        <.div(^.className := "navbar navbar-default",
          <.div(^.className := "container-fluid",
            <.div(^.className := "navbar-header",
              <.a(^.className := "navbar-brand", ^.href := "#", "Symbiotic")
            ),
            <.div(
              <.ul(Style.navMenu,
                P.menus.map(item =>
                  <.li(Style.menuItem(item.route.getClass == P.selectedPage.getClass),
                    item.tag.map { t =>
                      <.a(^.title := item.name, P.ctl setOnClick item.route,
                        t,
                        s" ${item.name}"
                      )
                    }.getOrElse {
                      <.a(item.name, P.ctl setOnClick item.route)
                    }

                  )
                )
              ),
              <.ul(^.className := "nav navbar-nav navbar-right",
                <.li(Style.menuItem(false),
                  <.a(^.onClick ==> B.doLogout, <.i(logout))
                )
              )
            )
          )
        )
      )
    )
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(props: Props, ref: js.UndefOr[String] = "", key: js.Any = {}) = component.set(key, ref)(props)

}


