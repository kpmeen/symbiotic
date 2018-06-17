package controllers

import com.google.inject.{Inject, Singleton}
import play.api.mvc._

@Singleton
class Application @Inject()(
    val controllerComponents: ControllerComponents
) extends SymbioticController {

  def serverInfo: Action[AnyContent] = Action { implicit request =>
    Ok(net.scalytica.symbiotic.server.BuildInfo.toJson).as("application/json")
  }

}
