/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import play.api.libs.json.Json
import play.api.mvc.Controller

trait SymbioticController extends Controller {

  val BadIdFormatResponse = BadRequest(Json.obj("msg" -> "Illegal Id format"))

}