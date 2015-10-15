/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import core.security.authentication.Authenticated
import models.project.{Project, ProjectId}
import play.api.libs.json._
import play.api.mvc._
import services.project.ProjectService

@Singleton
class ProjectController extends Controller {

  /**
   * Will try to get the Project with the provided ProjectId
   */
  def get(pid: String) = Authenticated { implicit request =>
    ProjectId.asOptId(pid).map(i =>
      ProjectService.findById(i).map(p => Ok(Json.toJson(p))).getOrElse(NotFound)
    ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal ID format")))
  }

  /**
   * Add a new Project
   */
  def add() = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Project](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(p) =>
        ProjectService.save(p)
        Created(Json.obj("msg" -> "successfully created new project"))
    }

  }

  /**
   * Update the Project with the given ProjectId
   */
  def update(pid: String) = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Project](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(project) =>
        ProjectId.asOptId(pid).map(i =>
          ProjectService.findById(i).map { p =>
            ProjectService.save(p)
            Ok(Json.obj("msg" -> "sucessfully updated project"))
          }.getOrElse(NotFound)
        ).getOrElse(BadRequest(Json.obj("msg" -> "Illegal ID format")))
    }
  }

}
