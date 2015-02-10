/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package controllers

import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

object BoardController extends Controller {


  def createBoard = Action.async { request =>
    Future.successful(NotImplemented)
  }

  def updateBoard = Action.async { request =>
    Future.successful(NotImplemented)
  }

  def removeBoard = Action.async { request =>
    Future.successful(NotImplemented)
  }

  def appendColumn = Action.async { request =>
    Future.successful(NotImplemented)
  }

  def insertColumn = Action.async { request =>
    Future.successful(NotImplemented)
  }

  def removeColumn = Action.async { request =>
    Future.successful(NotImplemented)
  }

  def moveColumn = Action.async { request =>
    Future.successful(NotImplemented)
  }

}
