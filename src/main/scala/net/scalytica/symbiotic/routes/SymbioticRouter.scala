package net.scalytica.symbiotic.routes

import java.util.UUID

import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Footer, TopNav}
import net.scalytica.symbiotic.css.GlobalStyle
import net.scalytica.symbiotic.models.{Menu, User}
import net.scalytica.symbiotic.pages.{HomePage, LoginPage}
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scalacss.ScalaCssReact._

object SymbioticRouter {

  val ServerBaseURI = "/symbiotic-server"

  sealed trait View

  case object Login extends View

  case class Home(oid: UUID) extends View

  case class Documents(fp: FolderPath) extends View

  case class Items(p: Item) extends View

  val TestCID = UUID.fromString("6a02be50-3dee-42f1-84fb-fbca1b9c4d70")

  val mainMenu = Vector(
    Menu("Home", Home(TestCID)),
    Menu("Documents", Documents(FolderPath(TestCID, None))),
    Menu("Items", Items(Item.Info))
  )

  def isAuthenticated = User.isLoggedIn

  val config = RouterConfigDsl[View].buildConfig { dsl =>
    import dsl._

    val secured = (emptyRule
      | dynamicRouteCT("home" / uuid.caseClass[Home]) ~> dynRender(h => HomePage())
      | Item.routes.prefixPath_/("items").pmap[View](Items) { case Items(p) => p }
      | DMan.routes.prefixPath_/("dman").pmap[View](Documents) { case Documents(fp) => fp }
      )
      .addCondition(isAuthenticated)(failed => Option(redirectToPage(Login)(Redirect.Push)))

    (trimSlashes
      | staticRoute(root, Login) ~> (if (!isAuthenticated) renderR(LoginPage.apply) else redirectToPage(Home(TestCID))(Redirect.Replace))
      | secured.prefixPath_/("#")
      )
      .notFound(redirectToPage(if (isAuthenticated) Home(TestCID) else Login)(Redirect.Replace))
      .renderWith((c, r) => if (isAuthenticated) securedLayout(c, r) else publicLayout(c, r))
  }

  def securedLayout(c: RouterCtl[View], r: Resolution[View]) = {
    <.div(GlobalStyle.appContent,
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      <.main(
        r.render()
      ),
      Footer()
    )
  }

  def publicLayout(c: RouterCtl[View], r: Resolution[View]) = r.render()

  val baseUrl = BaseUrl.fromWindowOrigin / "symbiotic"

  val router = Router(baseUrl, config.logToConsole)

}
