package controllers

import play.api.libs.json.Json
import play.api.mvc.Controller

trait SymbioticController extends Controller {

  val badIdFormatResponse = BadRequest(Json.obj("msg" -> "Illegal Id format"))

}
