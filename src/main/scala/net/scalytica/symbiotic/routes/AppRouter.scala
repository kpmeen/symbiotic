package net.scalytica.symbiotic.routes
import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Footer, TopNav}
import net.scalytica.symbiotic.models.{User, Menu}
import net.scalytica.symbiotic.pages.{HomePage, LoginPage}
import net.scalytica.symbiotic.core.converters.BooleanConverters._

object AppRouter {

  sealed trait AppPage

  case object Home extends AppPage

  case class Items(p: Item) extends AppPage

  val config = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._
    val itemRoutes: Rule = Item.routes.prefixPath_/("#items").pmap[AppPage](Items) { case Items(p) => p }
    (trimSlashes
      | staticRoute(root, Home) ~> renderR(r => User.isLoggedIn().option(HomePage()).getOrElse(LoginPage()))
      | itemRoutes
      ).notFound(redirectToPage(Home)(Redirect.Replace))
      .renderWith(layout)
  }

  val mainMenu = Vector(
    Menu("Home", Home),
    Menu("Items", Items(Item.Info))
  )

  def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) = {
    <.div(
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      r.render(),
      Footer()
    )
  }

  val baseUrl = BaseUrl.fromWindowOrigin / "index.html"

  val router = Router(baseUrl, config)

}
