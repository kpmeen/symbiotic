/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream

import core.docmanagement.{DocumentManagement, FileId, FileWrapper, Folder}
import models.customer.CustomerId
import models.parties.UserId
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

object DocumentManagementController extends Controller with FileStreaming {

  def getFileById(id: String) = Action { implicit request =>
    serve(DocumentManagement.getFileWrapper(FileId.asId(id)))
  }

  // TODO: Create a custom body parser that checks for the existence of the attached file...and handle appropriately.
  def upload(cidStr: String, destFolderStr: String) = Action(parse.multipartFormData) { implicit request =>

    // UserId should be placed as an implicit on the request in the Authenticated action.
    val tmpUserId = UserId.asId("550be36677c877d37345430e")

    val status = request.body.files.headOption.map { tmp =>
      FileWrapper(
        filename = tmp.filename,
        contentType = tmp.contentType,
        cid = CustomerId.asId(cidStr),
        folder = Option(Folder(destFolderStr)),
        stream = Option(new FileInputStream(tmp.ref.file))
      )
    }

    status.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>

      Logger.info(s"Going to save file $fw")

      DocumentManagement.save(tmpUserId, fw).fold(
        InternalServerError(Json.obj("msg" -> "bad things"))
      )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
    }
  }

}
