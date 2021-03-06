package net.scalytica.symbiotic.routing

import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.extra.router.StaticDsl.RouteB
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.{Footer, TopNav}
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.css.GlobalStyle
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.Menu
import net.scalytica.symbiotic.pages.{
  AuthCallbackPage,
  HomePage,
  LoginPage,
  UserProfilePage
}
import net.scalytica.symbiotic.routing.DMan.FolderURIElem

import scalacss.ScalaCssReact._

object SymbioticRouter {

  val ServerBaseURI = "/symbiotic-server"

  sealed trait View

  case object Login extends View

  //  case class Profile(uid: UserId) extends View
  case object Profile extends View

  case object Home extends View

  case class SocialAuthCallback(queryParams: String) extends View

  case class Library(fp: FolderURIElem) extends View

  val mainMenu = Vector(
    Menu("Home", Home, Some(<.i(GlobalStyle.home))),
    Menu(
      "Library",
      Library(FolderURIElem(None)),
      Some(<.i(GlobalStyle.library))
    ),
    Menu("My Profile", Profile, Some(<.i(GlobalStyle.profile)))
  )

  def queryParams: RouteB[SocialAuthCallback] =
    new RouteB("(.*)$", 1, g => Some(SocialAuthCallback(g(0))), _.queryParams)

  val config = RouterConfigDsl[View].buildConfig { dsl =>
    import dsl._

    val authRoute = dynamicRouteCT(queryParams) ~> dynRenderR {
      case (acb: SocialAuthCallback, ctl: RouterCtl[View]) =>
        AuthCallbackPage.apply(acb, ctl)
    }

    val secured = (emptyRule
      | staticRoute("home", Home) ~> render(HomePage())
      | DMan.routes.prefixPath_/("library").pmap[View](Library) {
        case Library(fp) => fp
      }
      | staticRoute("profile", Profile) ~> render(
        UserProfilePage(Session.userId.get)
      )).addCondition(CallbackTo(Session.hasToken))(
      failed => Option(redirectToPage(Login)(Redirect.Push))
    )

    (trimSlashes
      | staticRoute(root, Login) ~> (if (!Session.hasToken)
                                       renderR(LoginPage.apply)
                                     else
                                       redirectToPage(Home)(Redirect.Replace))
      | authRoute.prefixPath("#/authCallback")
      | secured.prefixPath_/("#")).notFound { path =>
      log.info(s"Could not find route for ${path.value}")
      redirectToPage(if (Session.hasToken) Home else Login)(Redirect.Replace)
    }.renderWith(
      (c, r) =>
        if (Session.hasToken) securedLayout(c, r) else publicLayout(c, r)
    )
  }

  def securedLayout(c: RouterCtl[View], r: Resolution[View]) = {
    <.div(
      GlobalStyle.appContent,
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      <.main(GlobalStyle.main, r.render()),
      Footer()
    )
  }

  def publicLayout(c: RouterCtl[View], r: Resolution[View]) = r.render()

  val baseUrl = BaseUrl.fromWindowOrigin / "symbiotic"

  val router = Router(baseUrl, config) //.logToConsole)

}
