package net.scalytica.symbiotic.routes

import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Footer, TopNav}
import net.scalytica.symbiotic.models.{Menu, User}
import net.scalytica.symbiotic.pages.{HomePage, LoginPage}

object SymbioticRouter {

  val ServerBaseURI = "/symbiotic-server"

  sealed trait View

  case object Login extends View

  case object Logout extends View

  case object Home extends View

  case class Items(p: Item) extends View

  val mainMenu = Vector(
    Menu("Home", Home),
    Menu("Items", Items(Item.Info))
  )

  def isAuthenticated = User.isLoggedIn

  val config = RouterConfigDsl[View].buildConfig { dsl =>
    import dsl._

    val secured = (emptyRule
      | staticRoute("home", Home) ~> render(HomePage())
      | Item.routes.prefixPath_/("items").pmap[View](Items) { case Items(p) => p }
      )
      .addCondition(isAuthenticated)(failed => Option(redirectToPage(Login)(Redirect.Replace)))

    (trimSlashes
      | staticRoute(root, Login) ~> renderR(LoginPage.apply)
      | staticRoute(root, Logout) ~> redirectToPage(Login)(Redirect.Replace)
      | secured.prefixPath_/("#secured")
      )
      .notFound(redirectToPage(if (isAuthenticated) Home else Login)(Redirect.Replace))
      .renderWith((c, r) => if (isAuthenticated) securedLayout(c, r) else publicLayout(c, r))
  }

  def securedLayout(c: RouterCtl[View], r: Resolution[View]) = <.div(
    TopNav(TopNav.Props(mainMenu, r.page, c)),
    r.render(),
    Footer()
  )

  def publicLayout(c: RouterCtl[View], r: Resolution[View]) = r.render()

  val baseUrl = BaseUrl.fromWindowOrigin / "symbiotic"

  val router = Router(baseUrl, config.logToConsole)

}
