/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import core.security.authentication.Authenticated
import models.party.Organisation
import models.party.PartyBaseTypes.OrganisationId
import play.api.libs.json._
import services.party.OrganisationService

@Singleton
class OrganisationController extends SymbioticController {

  /**
   * Will try to get the Organisation with the provided OrgId
   */
  def get(oid: String) = Authenticated { implicit request =>
    OrganisationId.asOptId(oid).map { i =>
      OrganisationService.findById(i).map(o => Ok(Json.toJson(o))).getOrElse(NotFound)
    }.getOrElse(BadIdFormatResponse)
  }

  /**
   * Add a new Organisation
   */
  def add = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Organisation](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(o) =>
        OrganisationService.save(o)
        Created(Json.obj("msg" -> "successfully created new organisation"))
    }

  }

  /**
   * Update the Organisation with the given OrgId
   */
  def update(pid: String) = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Organisation](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(organisation) =>
        OrganisationId.asOptId(pid).map { i =>
          OrganisationService.findById(i).map { o =>
            val org = organisation.copy(_id = o._id, id = o.id)
            OrganisationService.save(org)
            Ok(Json.obj("msg" -> "sucessfully updated organisation"))
          }.getOrElse(NotFound)
        }.getOrElse(BadIdFormatResponse)
    }
  }
}
