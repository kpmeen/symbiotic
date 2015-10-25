/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import core.security.authentication.Authenticated
import models.base.Id
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.project.{Member, MemberId, ProjectId}
import play.api.libs.json._
import play.api.mvc._
import services.project.MemberService

@Singleton
class MemberController extends SymbioticController {

  /**
   * Add a new Member
   */
  def add = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Member](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(m) =>
        MemberService.save(m)
        Created(Json.obj("msg" -> "successfully created new member"))
    }
  }

  /**
   * Update the Member with the given MemberId
   */
  def update(mid: String) = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Member](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(member) =>
        MemberId.asOptId(mid).map { i =>
          MemberService.findById(i).map { m =>
            val mbr = member.copy(_id = m._id, id = m.id)
            MemberService.save(mbr)
            Ok(Json.obj("msg" -> "sucessfully updated member"))
          }.getOrElse(NotFound)
        }.getOrElse(BadIdFormatResponse)
    }
  }

  private def getFor[A <: Id](id: A): Result = {
    val memberships = MemberService.findBy(id)
    if (memberships.nonEmpty) Ok(Json.toJson(memberships))
    else NoContent
  }

  /**
   * Get all Member entries for the given UserId
   */
  def getForUser(uid: String) = Authenticated { implicit request =>
    UserId.asOptId(uid).map(i => getFor(i)).getOrElse(BadIdFormatResponse)
  }

  /**
   * Get all Member entries for the given ProjectId
   */
  def getForProject(pid: String) = Authenticated { implicit request =>
    ProjectId.asOptId(pid).map(i => getFor(i)).getOrElse(BadIdFormatResponse)
  }

  /**
   * Get all Member entries for the given OrganisationId
   */
  def getForOrganisation(oid: String) = Authenticated { implicit request =>
    OrganisationId.asOptId(oid).map(i => getFor(i)).getOrElse(BadIdFormatResponse)
  }

  /**
   * Get the Member data for the given MemberId
   */
  def get(mid: String) = Authenticated { implicit request =>
    MemberId.asOptId(mid).map { i =>
      MemberService.findById(i).map { m =>
        Ok(Json.toJson(m))
      }.getOrElse(NotFound(Json.obj("msg" -> s"Could not find Member with Id $mid")))
    }.getOrElse(BadIdFormatResponse)
  }

  /**
   * Remove the Member data with the given MemberId...
   * This will break the relationship between a User and a Project.
   * In effect a User will no longer be able to access Project data.
   */
  def remove(mid: String) = Authenticated { implicit request =>
    NotImplemented
  }

}