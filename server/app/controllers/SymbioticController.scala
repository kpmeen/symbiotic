/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.party.User
import play.api.libs.json.Json

trait SymbioticController extends Silhouette[User, JWTAuthenticator] {

  val badIdFormatResponse = BadRequest(Json.obj("msg" -> "Illegal Id format"))

}
