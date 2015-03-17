/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import play.api.mvc.{Action, Controller}

object DocumentManagement extends Controller {

  def getFile(id: String) = Action { implicit request  =>
//    Ok.chunked(Document.serve(theDoc))
    ???
  }

}
