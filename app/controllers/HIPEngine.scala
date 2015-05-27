/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import hipe.HIPEService._
import hipe.core._
import models.parties.UserId
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}

/**
 * This controller defines service endpoints for interacting with
 * HIPE Processes. Including the handling of Tasks operations like
 * creating, delegating, completion, etc...
 *
 */
object HIPEngine extends Controller {

  // ************************************************************************
  // Process specifics services...
  // ************************************************************************

  def createProcess(name: String, strict: Boolean = false, description: Option[String]) = Action { implicit request =>
    val p = ProcessService.create(name, strict, description)
    Created(Json.toJson[Process](p))
  }

  def getProcessDefinition(procId: String) = Action { implicit request =>
    ProcessService.findById(procId).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
    )(p => Ok(Json.toJson[Process](p)))
  }

  def updateProcess(procId: String, name: Option[String], strict: Option[Boolean], desc: Option[String]) = Action { implicit request =>
    ProcessService.update(procId, name, strict, desc).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with id $procId for updating"))
    )(p => Ok(Json.toJson[Process](p)))
  }

  def removeProcess(procId: String) = Action { implicit request =>
    ProcessService.remove(procId)
    Ok(Json.obj("msg" -> s"Process with id $procId was removed"))
  }

  def addStep(procId: String) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(step) =>
        ProcessService.addStep(procId, step).fold(
          BadRequest(Json.obj("msg" -> s"Something went wrong adding trying to add a step to process $procId"))
        )(p => Ok(Json.toJson[Process](p)))
    }
  }

  def insertStepAt(procId: String, position: Int) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(step) =>
        ProcessService.addStepAt(procId, step, position).fold(
          NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
        )(p => Ok(Json.toJson[Process](p)))
    }
  }

  def moveStepTo(procId: String, from: Int, to: Int) = Action { implicit request =>
    ProcessService.moveStepTo(procId, from, to).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
    )(p => Ok(Json.toJson[Process](p)))
  }

  def removeStepAt(procId: String, at: Int) = Action { implicit request =>
    ProcessService.removeStepAt(procId, at).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
    )(p => Ok)
  }

  // ************************************************************************
  // Task services...
  // ************************************************************************

  def createTask(procId: ProcessId) = Action(parse.json) { implicit request =>
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) =>
        ProcessService.findById(procId)
          .flatMap(proc => TaskService.create(proc, task))
          .map(t => Created(Json.toJson[Task](t)))
          .getOrElse(NotFound(Json.obj("msg" -> s"Process $procId could not be found or it has no configured steps.")))
    }
  }

  def getTask(taskId: String) = Action { implicit request =>
    TaskService.findById(taskId).fold(
      NotFound(Json.obj("msg" -> s"Could not find task with Id $taskId"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def getTasksFor(procId: String) = Action { implicit request =>
    // TODO: Ensure that the process actually exists?
    val res = TaskService.findByProcessId(procId)
    if (res.isEmpty) NoContent
    else Ok(Json.toJson[Seq[Task]](res))
  }

  def moveTaskToNext(taskId: TaskId) = Action { implicit request =>
    TaskService.toNextStep(taskId) match {
      case Right(t) => Ok(Json.toJson[Task](t))
      case Left(err) => BadRequest(Json.obj("msg" -> err.msg))
    }
  }

  def moveTaskTo(taskId: TaskId, newStepId: StepId) = Action { implicit request =>

    def badReq(msg: String) = BadRequest(Json.obj("msg" -> msg))

    TaskService.toStep(taskId, newStepId) match {
      case Right(t) => Ok(Json.toJson[Task](t))
      case Left(err) =>
        err match {
          case FailureTypes.NotFound(msg) => NotFound(Json.obj("msg" -> msg))
          case FailureTypes.NotPossible(msg) => badReq(msg)
          case FailureTypes.Incomplete(msg) => badReq(msg)
          case FailureTypes.NotAllowed(msg) => Forbidden(Json.obj("msg" -> msg))
          case oops => InternalServerError(Json.obj("msg" -> oops.msg))
        }
    }
  }

  def update(taskId: String) = Action(parse.json) { implicit request =>
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) =>
        // TODO: Do some validation against the original data
        TaskService.update(taskId, task).fold(
          InternalServerError(Json.obj("msg" -> "Could not find task after update"))
        )(t => Ok(Json.toJson[Task](t)))
    }
  }

  /**
   * Complete a users assignment for a given task.
   *
   * TODO: Remove userId arg...should be the currently logged in user.
   */
  def complete(taskId: String) = Action { implicit request =>
    request.getQueryString("userId").map(UserId.asId).map { uid =>
      TaskService.complete(taskId, uid).fold(
        InternalServerError(Json.obj("msg" -> "Could not find task after completing assignment"))
      )(t => Ok(Json.toJson[Task](t)))
    }.getOrElse(BadRequest(Json.obj("msg" -> "For now...the complete function requires a req param called userId")))
  }

  def approve(taskId: String) = Action { implicit request =>
    TaskService.approveTask(taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete approve operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def reject(taskId: String) = Action { implicit request =>
    TaskService.rejectTask(taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete reject operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def claim(taskId: String, toUser: String) = assign(taskId, toUser)

  // TODO: Handle error scenarios in a good way.
  def assign(taskId: String, toUser: String) = Action { implicit request =>
    TaskService.assignTo(taskId, toUser).fold(
      BadRequest(Json.obj("msg" -> "Boo boo..."))
    )(t => Ok(Json.toJson[Task](t)))
  }

  // TODO: Exactly the same as above...OR, should we _append_ the assignee to a stack of assignees?
  def delegate(taskId: String, toUser: String) = Action { implicit request =>
    TaskService.assignTo(taskId, toUser).fold(
      BadRequest(Json.obj("msg" -> "Boo boo..."))
    )(t => Ok(Json.toJson[Task](t)))
  }

}
