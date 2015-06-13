package net.scalytica.symbiotic.routes

import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Footer, TopNav}
import net.scalytica.symbiotic.models.{Menu, User}
import net.scalytica.symbiotic.pages.{HomePage, LoginPage}

object SymbioticRouter {

  sealed trait View

  case object Login extends View

  case object Home extends View

  case class Items(p: Item) extends View

  val mainMenu = Vector(
    Menu("Home", Home),
    Menu("Items", Items(Item.Info))
  )

  val config = RouterConfigDsl[View].buildConfig { dsl =>
    import dsl._

    val secured = (emptyRule
      | staticRoute("#home", Home) ~> render(HomePage())
      | Item.routes.prefixPath_/("#items").pmap[View](Items) { case Items(p) => p }
      )
      .addCondition(User.isLoggedIn)(failed => Option(redirectToPage(Login)(Redirect.Replace)))

    (trimSlashes
      | staticRoute("#login", Login) ~> renderR(r => LoginPage(r))
      | secured
      )
      .notFound(redirectToPage(if (User.isLoggedIn) Home else Login)(Redirect.Replace))
      .renderWith(layout)
  }

  def layout(c: RouterCtl[View], r: Resolution[View]) = {
    <.div(
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      r.render(),
      Footer()
    )
  }

  val baseUrl = BaseUrl.fromWindowOrigin / "symbiotic" //"index.html"

  val router = Router(baseUrl, config.logToConsole)

}
