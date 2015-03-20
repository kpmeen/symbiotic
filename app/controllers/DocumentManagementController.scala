/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import core.docmanagement.{FileId, DocumentManagement}
import play.api.mvc.{Action, Controller}

import play.api.libs.concurrent.Execution.Implicits.defaultContext

object DocumentManagementController extends Controller with FileStreaming {

  def getFile(id: String) = Action { implicit request  =>
    val fw = DocumentManagement.getFileWrapper(FileId.asId(id))

    serve(fw)
  }

}
