/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import hipe.core._
import org.bson.types.ObjectId
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}

/**
 * This controller defines service endpoints for interacting with HIPE Processes. Including the handling
 * of Tasks operations like creating, delegating, completion, etc...
 */
object HIPEngine extends Controller with ProcessOperations with TaskOperations {

  // ************************************************************************
  // Process specifics services...
  // ************************************************************************

  def createProcess(name: String, strict: Boolean = false, description: Option[String]) = Action { implicit request =>
    val p = Process(
      id = Some(ProcessId(new ObjectId().toString)),
      name = name,
      strict = strict,
      description = description
    )
    Process.save(p)
    Created(Json.toJson[Process](p))
  }

  def getProcessDefinition(procId: String) = Action { implicit request =>
    Process.findById(procId).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
    )(p => Ok(Json.toJson[Process](p)))
  }

  def updateProcess(procId: String, name: Option[String], strict: Option[Boolean], description: Option[String]) = Action { implicit request =>
    // TODO: Only update the metadata...
    applyAndSaveProcess(procId)(p => Option(p.copy(
      name = name.fold(p.name)(n => if (n.nonEmpty) n else p.name),
      strict = strict.getOrElse(p.strict),
      description = description.orElse(p.description)
    ))).fold(
        NotFound(Json.obj("msg" -> s"Could not find process with id $procId for updating"))
      )(p => Ok(Json.toJson[Process](p)))
  }

  def removeProcess(procId: String) = Action { implicit request =>
    Process.delete(procId)
    Ok(Json.obj("msg" -> s"Process with id $procId was removed"))
  }

  def addStep(procId: String) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: This typically renders horrible error messages. Improve!
      case Right(step) => applyAndSaveProcess(procId)(proc =>
        Option(appendStep(proc, step))
      ).fold(
          BadRequest(Json.obj("msg" -> s"Something went wrong adding trying to add a step to process $procId"))
        )(p => Ok(Json.toJson[Process](p)))
    }
  }

  def insertStepAt(procId: String, position: Int) = Action(parse.json) { implicit request =>
    request.body.validate[Step].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: This typically renders horrible error messages. Improve!
      case Right(step) => applyAndSaveProcess(procId) { proc =>
        Option(insertStep(proc, step, position))
      }.fold(
          NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
        )(p => Ok(Json.toJson[Process](p)))
    }
  }

  def moveStepTo(procId: String, from: Int, to: Int) = Action { implicit request =>
    applyAndSaveProcess(procId)(p => Option(moveStep(p, from, to))).fold(
      NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
    )(p => Ok(Json.toJson[Process](p)))
  }

  def removeStepAt(procId: String, at: Int) = Action { implicit request =>
    applyAndSaveProcess(procId) { p =>
      removeStep(p, at)((pid, sid) => Task.findByProcessId(pid).filter(t => t.stepId == sid))
    }.fold(
        NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
      )(p => Ok)
  }

  private[this] def applyAndSaveProcess(procId: ProcessId)(f: Process => Option[Process]): Option[Process] =
    Process.findById(procId).flatMap(p => f(p).map { pa =>
      Process.save(pa)
      pa
    })

  // ************************************************************************
  // Task services...
  // ************************************************************************

  def createTask(procId: ProcessId) = Action(parse.json) { implicit request =>
    request.body.validate[Task].asEither match {
      case Left(jserr) => BadRequest(JsError.toFlatJson(jserr)) // TODO: IMprove me...I'm horrible
      case Right(task) => Process.findById(procId).flatMap { p =>
        addTaskToProcess(p, task).map { t =>
          Task.save(t)
          t
        }
      }.fold(
          NotFound(Json.obj("msg" -> s"Could not find process with Id $procId"))
        )(t => Created(Json.toJson[Task](t)))
    }
  }

  def getTask(taskId: String) = Action { implicit request =>
    Task.findById(taskId).fold(
      NotFound(Json.obj("msg" -> s"Could not find task with Id $taskId"))
    )(t => Ok(Json.toJson[Task](t)))
  }

  def getTasksFor(procId: String) = Action { implicit request =>
    // TODO: Ensure that the process actually exists?
    val res = Task.findByProcessId(procId)
    if (res.isEmpty) NoContent
    else Ok(Json.toJson[List[Task]](res))
  }

  def moveTaskTo(procId: String, taskId: TaskId, newStepId: StepId) = Action { implicit request =>
    Process.findById(procId).flatMap(p =>
      Task.findById(taskId).flatMap(t =>
        moveTask(p, t, newStepId).map { t =>
          Task.save(t)
          t
        }
      )).fold(
        NotFound(Json.obj("msg" -> s"Either Process with Id $procId or task with Id $taskId was not found"))
      )(t => Ok(Json.toJson[Task](t)))
  }

  def update(taskId: String) = Action(parse.json) { implicit request =>
    ???
  }

  def complete(taskId: String) = Action(parse.json) { implicit request =>
    ???
  }

  def reject(taskId: String) = Action(parse.json) { implicit request =>
    ???
  }

  def assignTask() = Action { implicit request =>
    ???
  }

  def delegateTask() = Action { implicit request =>
    ???
  }

  private[this] def applyAndSaveTask(taskId: TaskId)(f: Task => Option[Task]): Option[Task] = ???

}
