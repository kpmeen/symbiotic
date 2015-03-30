/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import play.api.mvc.{Action, Controller}

object ProcessController extends Controller {

  //***************************************************
  // Process specifics
  def createProcess() = Action { implicit request =>
    ???
  }

  def getProcessDefinition = Action { implicit request =>
    ???
  }

  // TODO: Only update the metadata...
  def updateProcess() = Action { implicit request =>
    ???
  }

  def removeProcess() = Action { implicit request =>
    ???
  }

  //***************************************************
  // process Step related services... there is no single step read service since all data is available through the
  // get process service.
  def createStep() = Action { implicit request =>
    ???
  }

  def appendStep() = Action { implicit request =>
    ???
  }

  def insertStepAt() = Action { implicit request =>
    ???
  }

  def moveStep() = Action { implicit request =>
    ???
  }

  def removeStep() = Action { implicit request =>
    ???
  }

  //***************************************************
  // Task services...
  def createTask() = Action { implicit request =>
    ???
  }

  def getTask = Action { implicit request =>
    ???
  }

  def getTasksFor = Action { implicit request =>
    ???
  }

  def moveTask() = Action { implicit request =>
    ???
  }

  def assignTask() = Action { implicit request =>
    ???
  }

  def delegateTask() = Action { implicit request =>
    ???
  }

}
