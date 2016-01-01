/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject.Singleton

import core.lib.{Failure, Success}
import core.security.authentication.Authenticated
import models.project.{Project, ProjectId}
import play.api.libs.json._
import repository.mongodb.project.MongoDBProjectRepository

@Singleton
class ProjectController extends SymbioticController {

  /**
   * Will try to get the Project with the provided ProjectId
   */
  def get(pid: String) = Authenticated { implicit request =>
    ProjectId.asOptId(pid).map { i =>
      MongoDBProjectRepository.findById(i).map(p => Ok(Json.toJson(p))).getOrElse(NotFound)
    }.getOrElse(badIdFormatResponse)
  }

  /**
   * Add a new Project
   */
  def add = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Project](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(p) =>
        MongoDBProjectRepository.save(p) match {
          case s: Success => Created(Json.obj("msg" -> "Successfully created new project"))
          case Failure(msg) => InternalServerError(Json.obj(msg -> msg))
        }
    }
  }

  /**
   * Update the Project with the given ProjectId
   */
  def update(pid: String) = Authenticated(parse.json) { implicit request =>
    Json.fromJson[Project](request.body).asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(JsError(jserr)))
      case Right(project) =>
        ProjectId.asOptId(pid).map { i =>
          MongoDBProjectRepository.findById(i).map { p =>
            val prj = project.copy(_id = p._id, id = p.id)
            MongoDBProjectRepository.save(prj) match {
              case s: Success => Ok(Json.obj("msg" -> "Successfully updated project"))
              case Failure(msg) => InternalServerError(Json.obj(msg -> msg))
            }
          }.getOrElse(NotFound)
        }.getOrElse(badIdFormatResponse)
    }
  }

}
