package net.scalytica.symbiotic.routing

import java.util.UUID

import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Footer, TopNav}
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.css.GlobalStyle
import net.scalytica.symbiotic.models.Menu
import net.scalytica.symbiotic.pages.{HomePage, LoginPage, UserProfilePage}
import net.scalytica.symbiotic.routing.DMan.FolderPath

import scalacss.ScalaCssReact._

object SymbioticRouter {

  val ServerBaseURI = "/symbiotic-server"

  sealed trait View

  case object Login extends View

  //  case class Profile(uid: UserId) extends View
  case object Profile extends View

  case class Home(oid: UUID) extends View

  case class Library(fp: FolderPath) extends View

  val TestOrgId = UUID.fromString("a6c381d5-fa3e-4541-b0c4-1942834768e2")

  val mainMenu = Vector(
    Menu("Home", Home(TestOrgId), Some(<.i(GlobalStyle.home))),
    Menu("Library", Library(FolderPath(TestOrgId, None)), Some(<.i(GlobalStyle.library))),
    Menu("My Profile", Profile, Some(<.i(GlobalStyle.profile)))
  )

  def isAuthenticated = Session.validate

  val config = RouterConfigDsl[View].buildConfig { dsl =>
    import dsl._

    val secured = (emptyRule
      | dynamicRouteCT("home" / uuid.caseClass[Home]) ~> dynRender(h => HomePage())
      | DMan.routes.prefixPath_/("library").pmap[View](Library) { case Library(fp) => fp }
      | staticRoute("profile", Profile) ~> render(UserProfilePage(Session.userId.get))
      )
      .addCondition(CallbackTo(isAuthenticated))(failed => Option(redirectToPage(Login)(Redirect.Push)))

    (trimSlashes
      | staticRoute(root, Login) ~> (if (!isAuthenticated) renderR(LoginPage.apply) else redirectToPage(Home(TestOrgId))(Redirect.Replace))
      | secured.prefixPath_/("#")
      )
      .notFound(redirectToPage(if (isAuthenticated) Home(TestOrgId) else Login)(Redirect.Replace))
      .renderWith((c, r) => if (isAuthenticated) securedLayout(c, r) else publicLayout(c, r))
  }

  def securedLayout(c: RouterCtl[View], r: Resolution[View]) = {
    <.div(GlobalStyle.appContent,
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      <.main(GlobalStyle.main,
        r.render()
      ),
      Footer()
    )
  }

  def publicLayout(c: RouterCtl[View], r: Resolution[View]) = r.render()

  val baseUrl = BaseUrl.fromWindowOrigin / "symbiotic"

  val router = Router(baseUrl, config)//.logToConsole)

}
