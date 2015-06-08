/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import hipe.HIPEService.ProcessService.MoveStepCommands._
import hipe.HIPEService._
import hipe.core.{HIPEResult, _}
import models.parties.UserId
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, Json, Writes}
import play.api.mvc.{Action, Controller, Result}

/**
 * This controller defines service endpoints for interacting with HIPE Processes.
 * Including the handling of Tasks operations like creating, delegating, completion, etc...
 */
class HIPEngine extends Controller {

  val logger = LoggerFactory.getLogger(this.getClass)

  private def handle[A](hipeRes: HIPEResult[A])(implicit writes: Writes[A]): Result = {
    hipeRes match {
      case Right(res) => Ok(Json.toJson(res))
      case Left(err) =>
        err match {
          case FailureTypes.BadArgument(msg) => BadRequest(Json.obj("msg" -> msg))
          case FailureTypes.Incomplete(msg) => NotAcceptable(Json.obj("msg" -> msg))
          case FailureTypes.NotAllowed(msg) => Forbidden(Json.obj("msg" -> msg))
          case FailureTypes.NotFound(msg) => NotFound(Json.obj("msg" -> msg))
          case FailureTypes.NotPossible(msg) => NotAcceptable(Json.obj("msg" -> msg))
          case ft => InternalServerError(Json.obj("msg" -> s"An error occured: ${ft.msg}"))
        }
    }
  }

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
    )(p => {
      logger.debug(s"Returning process $p")
      Ok(Json.toJson[Process](p))
    })
  }

  def updateProcess(procId: String, name: Option[String], strict: Option[Boolean], desc: Option[String]) = Action { implicit request =>
    handle[Process](ProcessService.update(procId, name, strict, desc))
  }

  def removeProcess(procId: String) = Action(parse.anyContent) { implicit request =>
    ProcessService.remove(procId)
    Ok(Json.obj("msg" -> s"Process with id $procId was removed"))
  }

  def appendStep(procId: String) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(step) => handle[Process](ProcessService.addStep(procId, step))
    }
  }

  def insertStepAt(procId: String, position: Int) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(step) => handle[Process](ProcessService.addStepAt(procId, step, position))
    }
  }

  def moveGroupTo(procId: String, groupId: String, to: Int) = Action { implicit request =>
    handle[Process](ProcessService.moveGroupTo(procId, groupId, to))
  }

  def moveStepTo(procId: String, sid: String) = Action(parse.json) { implicit request =>
    request.body.validate[MoveStep].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(cmd) => handle[Process](ProcessService.moveStepTo(cmd))
    }
  }

  def removeStep(procId: String, sid: String) = Action(parse.anyContent) { implicit request =>
    handle[Process](ProcessService.removeStepAt(procId, sid))
  }

  def removeGroup(procId: String, groupId: String) = Action(parse.anyContent) { implicit request =>
    handle[Process](ProcessService.removeGroupAt(procId, groupId))
  }

  // ************************************************************************
  // Task services...
  // ************************************************************************

  def createTask(procId: ProcessId) = Action(parse.json) { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) =>
        ProcessService.findById(procId)
          .flatMap(proc => TaskService.create(dummyUser, proc, task))
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
    val dummyUser = UserId.create() // FIXME: Use user from session
    handle[Task](TaskService.toNextStep(dummyUser, taskId))
  }

  def moveTaskTo(taskId: TaskId, newStepId: StepId) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    handle[Task](TaskService.toStep(dummyUser, taskId, newStepId))
  }

  def update(taskId: String) = Action(parse.json) { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) =>
        // TODO: Do some validation against the original data
        TaskService.update(dummyUser, taskId, task).fold(
          InternalServerError(Json.obj("msg" -> "Could not find task after update"))
        )(t => Ok(Json.toJson[Task](t)))
    }
  }

  /**
   * Complete a users assignment for a given task.
   */
  def complete(taskId: String) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    TaskService.complete(dummyUser, taskId).fold(
      InternalServerError(Json.obj("msg" -> "Could not find task after completing assignment"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def approve(taskId: String) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    TaskService.approveTask(dummyUser, taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete approve operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def reject(taskId: String) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    TaskService.rejectTask(dummyUser, taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete reject operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def claim(taskId: String, toUser: String) = assign(taskId, toUser)

  // TODO: Handle error scenarios in a good way.
  def assign(taskId: String, toUser: String) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    TaskService.assignTo(dummyUser, taskId, toUser).fold(
      BadRequest(Json.obj("msg" -> "Boo boo..."))
    )(t => Ok(Json.toJson[Task](t)))
  }

  // TODO: Handle error scenarios in a good way.
  def delegate(taskId: String, toUser: String) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    TaskService.assignTo(dummyUser, taskId, toUser).fold(
      BadRequest(Json.obj("msg" -> "Boo boo..."))
    )(t => Ok(Json.toJson[Task](t)))
  }

}
