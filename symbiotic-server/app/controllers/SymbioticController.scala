package controllers

import play.api.libs.json.Json
import play.api.mvc.BaseController

import scala.concurrent.ExecutionContext

trait SymbioticController extends BaseController {

  val badIdFormatResponse = BadRequest(Json.obj("msg" -> "Illegal Id format"))

  implicit def ec: ExecutionContext = defaultExecutionContext

}
