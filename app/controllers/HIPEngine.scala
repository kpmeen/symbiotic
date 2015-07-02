/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import javax.inject._

import hipe.HIPEService._
import hipe.MoveStepCommands._
import hipe.core.{HIPEResult, _}
import models.parties.UserId
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, Json, Writes}
import play.api.mvc.{Action, Controller, Result}

/**
 * This controller defines service endpoints for interacting with HIPE Processes.
 * Including the handling of Tasks operations like creating, delegating, completion, etc...
 */
@Singleton
class HIPEngine @Inject()(taskService: TaskService, processService: ProcessService) extends Controller {

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
    val p = processService.create(name, strict, description)
    Created(Json.toJson[Process](p))
  }

  def getProcessDefinition(procId: String) = Action { implicit request =>
    processService.findById(procId).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
    )(p => {
      logger.debug(s"Returning process $p")
      Ok(Json.toJson[Process](p))
    })
  }

  def updateProcess(procId: String, name: Option[String], strict: Option[Boolean], desc: Option[String]) = Action { implicit request =>
    handle[Process](processService.update(procId, name, strict, desc))
  }

  def removeProcess(procId: String) = Action(parse.anyContent) { implicit request =>
    processService.remove(procId)
    Ok(Json.obj("msg" -> s"Process with id $procId was removed"))
  }

  def appendStep(procId: String) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(step) => handle[Process](processService.addStep(procId, step))
    }
  }

  def insertStepAt(procId: String, position: Int) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(step) => handle[Process](processService.addStepAt(procId, step, position))
    }
  }

  def moveGroupTo(procId: String, groupId: String, to: Int) = Action { implicit request =>
    handle[Process](processService.moveGroupTo(procId, groupId, to))
  }

  def moveStepTo(procId: String, sid: String) = Action(parse.json) { implicit request =>
    request.body.validate[MoveStep].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(cmd) => handle[Process](processService.moveStepTo(cmd))
    }
  }

  def removeStep(procId: String, sid: String) = Action(parse.anyContent) { implicit request =>
    handle[Process](processService.removeStepAt(procId, sid))
  }

  def removeGroup(procId: String, groupId: String) = Action(parse.anyContent) { implicit request =>
    handle[Process](processService.removeGroupAt(procId, groupId))
  }

  // ************************************************************************
  // Task services...
  // ************************************************************************

  def createTask(procId: ProcessId) = Action(parse.json) { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) => processService.findById(procId)
        .flatMap(proc => taskService.create(dummyUser, proc, task))
        .map(t => Created(Json.toJson[Task](t)))
        .getOrElse(NotFound(Json.obj("msg" -> s"Process $procId could not be found or it has no configured steps.")))
    }
  }

  def getTask(taskId: String) = Action { implicit request =>
    taskService.findById(taskId).fold(
      NotFound(Json.obj("msg" -> s"Could not find task with Id $taskId"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def getTasksFor(procId: String) = Action { implicit request =>
    // TODO: Ensure that the process actually exists?
    val res = taskService.findByProcessId(procId)
    if (res.isEmpty) NoContent
    else Ok(Json.toJson[Seq[Task]](res))
  }

  def moveTaskToNext(taskId: TaskId) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    handle[Task](taskService.toNextStep(dummyUser, taskId))
  }

  def moveTaskToPrev(taskId: TaskId) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    handle[Task](taskService.toPreviousStep(dummyUser, taskId))
  }

  def moveTaskTo(taskId: TaskId, newStepId: StepId) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    handle[Task](taskService.toStep(dummyUser, taskId, newStepId))
  }

  def update(taskId: String) = Action(parse.json) { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) =>
        // TODO: Do some validation against the original data
        //        taskService.update(dummyUser, taskId, task).fold(
        //          InternalServerError(Json.obj("msg" -> "Could not find task after update"))
        //        )(t => Ok(Json.toJson[Task](t)))
        NotImplemented
    }
  }

  /**
   * Complete a users assignment for a given task.
   */
  def complete(taskId: String) = Action { implicit request =>
    val dummyUser = UserId("DarthVader") // FIXME: Use user from session
    taskService.complete(dummyUser, taskId).fold(
      InternalServerError(Json.obj("msg" -> "Could not find task after completing assignment"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def approve(taskId: String) = Action { implicit request =>
    val dummyUser = UserId("DarthVader") // FIXME: Use user from session
    taskService.approveTask(dummyUser, taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete approve operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def reject(taskId: String) = Action { implicit request =>
    val dummyUser = UserId("DarthVader") // FIXME: Use user from session
    taskService.rejectTask(dummyUser, taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete reject operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def decline(taskId: String) = Action { implicit request =>
    val dummyUser = UserId("DarthVader") // FIXME: Use user from session
    taskService.declineTask(dummyUser, taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete decline operation"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def consolidate(taskId: String) = Action { implicit request =>
    val dummyUser = UserId("DarthVader") // FIXME: Use user from session
    taskService.consolidateTask(dummyUser, taskId).fold(
      BadRequest(Json.obj("msg" -> "Could not complete consolidate operation"))
    )(t => Ok(Json.toJson[Task(t)] ) )
  }

  def claim(taskId: String, toUser: String) = assign(taskId, toUser)

  // TODO: Handle error scenarios in a good way.
  def assign(taskId: String, toUser: String) = Action { implicit request =>
    val dummyUser = UserId.create() // FIXME: Use user from session
    taskService.assignTo(dummyUser, taskId, toUser).fold(
      BadRequest(Json.obj("msg" -> "Boo boo..."))
    )(t => Ok(Json.toJson[Task](t)))
  }

  // TODO: Handle error scenarios in a good way.
  def delegate(taskId: String, toUser: String) = Action { implicit request =>
    val dummyUser = UserId("DarthVader") // FIXME: Use user from session
    taskService.assignTo(dummyUser, taskId, toUser).fold(
      BadRequest(Json.obj("msg" -> "Boo boo..."))
    )(t => Ok(Json.toJson[Task](t)))
  }

}
