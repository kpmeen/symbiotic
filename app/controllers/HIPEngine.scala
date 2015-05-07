/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import hipe.HIPEService._
import hipe.core._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}

/**
 * This controller defines service endpoints for interacting with HIPE Processes. Including the handling
 * of Tasks operations like creating, delegating, completion, etc...
 *
 * FIXME: OK...it was wrong to move all logic to the controller. Since the DSL parser needs access to
 * the actual functions too. Re-introduce the HIPEService? Or can I use func composition? no...or?
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

  def updateProcess(procId: String, name: Option[String], strict: Option[Boolean], description: Option[String]) = Action { implicit request =>
    ProcessService.update(procId, name, strict, description).fold(
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
      case Right(task) => TaskService.create(procId, task).fold(
        NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
      )(t => Created(Json.toJson[Task](t)))
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

  def moveTaskTo(procId: String, taskId: TaskId, newStepId: StepId) = Action { implicit request =>
    TaskService.moveTo(procId, taskId, newStepId).fold(
      NotFound(Json.obj("msg" -> s"Either Process with Id $procId or task with Id $taskId was not found"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def update(taskId: String) = Action(parse.json) { implicit request =>
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: horrible error messages. Improve!
      case Right(task) =>
        // TODO: Do some validation against the original data
        TaskService.update(taskId, task).fold(
          InternalServerError(Json.obj("msg" -> "Could not find task after update"))
        )(t =>
          Ok(Json.toJson[Task](t))
          )
    }
  }

  /**
   * Same as moving a task to the right on a KanBan board.
   */
  def complete(taskId: String) = Action(parse.json) { implicit request =>
    ???
  }

  /**
   * Same as moving a task to the left on a KanBan board
   */
  def reject(taskId: String) = Action(parse.json) { implicit request =>
    ???
  }

  // TODO: Handle error scenarios in a good way.
  def assign(taskId: String, toUser: String) = Action { implicit request =>
    TaskService.reassign(taskId, toUser).fold(
      ???
    )(t => Ok(Json.toJson[Task](t)))
  }

  // TODO: Exactly the same as above...OR, should we _append_ the assignee to a stack of assignees?
  def delegate(taskId: String, toUser: String) = Action { implicit request =>
    TaskService.reassign(taskId, toUser).fold(
      ???
    )(t => Ok(Json.toJson[Task](t)))
  }

}
