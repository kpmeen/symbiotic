/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import core.lib.{Failure, Success}
import core.security.authentication.Authenticated
import models.party.Organisation
import models.party.PartyBaseTypes.OrganisationId
import play.api.libs.json._
import services.party.OrganisationService

@Singleton
class OrganisationController @Inject() (val orgService: OrganisationService) extends SymbioticController {

  /**
   * Will try to get the Organisation with the provided OrgId
   */
  def get(oid: String) = Authenticated { implicit request =>
    OrganisationId.asOptId(oid).map { i =>
      orgService.findById(i).map(o => Ok(Json.toJson(o))).getOrElse(NotFound)
    }.getOrElse(badIdFormatResponse)
  }

  /**
   * Add a new Organisation
   */
  def add = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Organisation](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(o) =>
        orgService.save(o) match {
          case s: Success => Created(Json.obj("msg" -> "Successfully created new organisation"))
          case Failure(msg) => InternalServerError(Json.obj("msg" -> msg))
        }
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
          orgService.findById(i).map { o =>
            val org = organisation.copy(_id = o._id, id = o.id)
            orgService.save(org) match {
              case s: Success => Ok(Json.obj("msg" -> "Successfully updated organisation"))
              case Failure(msg) => InternalServerError(Json.obj("msg" -> msg))
            }

          }.getOrElse(NotFound)
        }.getOrElse(badIdFormatResponse)
    }
  }
}
