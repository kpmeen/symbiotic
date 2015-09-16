/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import models.party.Organisation
import models.party.PartyBaseTypes.OrgId
import play.api.libs.json._
import play.api.mvc._

@Singleton
class OrganisationController extends Controller {

  /**
   * Will try to get the Organisation with the provided OrgId
   */
  def get(oid: String) = Action { implicit request =>
    OrgId.asOptId(oid).map(i =>
      Organisation.findById(i).map(o => Ok(Json.toJson(o))).getOrElse(NotFound)
    ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal ID format")))
  }

  /**
   * Add a new Organisation
   */
  def add() = Action(parse.json) { implicit request =>
    Json.fromJson[Organisation](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(o) =>
        Organisation.save(o)
        Created(Json.obj("msg" -> "successfully created new organisation"))
    }

  }

  /**
   * Update the Organisation with the given OrgId
   */
  def update(pid: String) = Action(parse.json) { implicit request =>
    Json.fromJson[Organisation](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(org) =>
        OrgId.asOptId(pid).map(i =>
          Organisation.findById(i).map { o =>
            Organisation.save(o)
            Ok(Json.obj("msg" -> "sucessfully updated organisation"))
          }.getOrElse(NotFound)
        ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal ID format")))
    }
  }
}
