package controllers

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api._
import core.security.authentication.JWTEnvironment
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._

@Singleton
class Application @Inject()(
    val messagesApi: MessagesApi,
    val env: Environment[JWTEnvironment]
) extends SymbioticController {

  private val log: Logger = Logger(this.getClass)

  def serverInfo = Action { implicit request =>
    Ok(net.scalytica.symbiotic.server.BuildInfo.toJson).as("application/json")
  }

}
