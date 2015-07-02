/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream

import dman._
import models.customer.CustomerId
import models.parties.UserId
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class DocumentManagement extends Controller with DocManOperations with FileStreaming {

  private[this] val logger = Logger(this.getClass)

  val dummyUser = UserId.create()

  private[this] def getTree(cid: CustomerId, path: Option[String], includeFiles: Boolean) = {
    val from = path.map(Folder.apply).getOrElse(Folder.rootFolder)
    if (includeFiles) {
      val twf = treeWithFiles(cid, from)
      if (twf.isEmpty) NoContent else Ok(Json.toJson(twf))
    }
    else {
      val folders = treeNoFiles(cid, from)
      if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
    }
  }

  def getRootTree(customerId: String, includeFiles: Boolean = false) = Action { implicit request =>
    getTree(customerId, None, includeFiles)
  }

  def getSubTree(customerId: String, path: String, includeFiles: Boolean = false) = Action { implicit request =>
    getTree(customerId, Option(path), includeFiles)
  }

  def getDirectDescendants(customerId: String, path: String) = Action { implicit request =>
    val cwf = childrenWithFiles(customerId, Folder(path))
    if (cwf.isEmpty) NoContent else Ok(Json.toJson(cwf))
  }

  def showFiles(customerId: String, path: String) = Action { implicit request =>
    val lf = listFiles(customerId, Folder(path))
    if (lf.isEmpty) NoContent else Ok(Json.toJson(lf))
  }

  def lock(fileId: String) = Action { implicit request =>
    // TODO: Improve return types from lockFile to be able to provide better error handling
    lockFile(dummyUser, fileId)
      .map(l => Ok(Json.toJson(l)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not lock file $fileId")))
  }

  def unlock(fileId: String) = Action { implicit request =>
    // TODO: Improve return types from unlockFile to be able to provide better error handling
    if (unlockFile(dummyUser, fileId)) Ok(Json.obj("msg" -> s"File $fileId is now unlocked"))
    else BadRequest(Json.obj("msg" -> s"Could not unlock file $fileId"))
  }

  def isLocked(fileId: String) = Action { implicit request =>
    Ok(Json.obj("hasLock" -> hasLock(fileId)))
  }

  def addFolder(customerId: String, fullPath: String, createMissing: Boolean = true) = Action { implicit request =>
    // TODO: Improve return types from createFolder to be able to provide better error handling
    createFolder(customerId, Folder(fullPath), createMissing)
      .map(fid => Created(Json.toJson(fid)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not create folder at $fullPath")))
  }

  def changeFolderName(customerId: String, orig: String, mod: String) = Action { implicit request =>
    val renamed = renameFolder(customerId, Folder(orig), Folder(mod))
    if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
  }

  def moveFolderTo(customerId: String, orig: String, mod: String) = Action { implicit request =>
    val moved = moveFolder(customerId, Folder(orig), Folder(mod))
    if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
  }

  def moveFileTo(fileId: String, orig: String, dest: String) = Action { implicit request =>
    // TODO: Improve return types from moveFile to be able to provide better error handling
    moveFile(fileId, Folder(orig), Folder(dest))
      .map(fw => Ok(Json.toJson(fw)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not move the file with id $fileId from $orig to $dest")))
  }

  /*
   TODO: #1 - Create a custom body parser that checks for the existence of the attached file...and handle appropriately.
   TODO: #2 - Integrate with ClammyScan
   TODO: #3 - Evaluate possibility of streaming upload...maybe it will be supported in play 2.4? What if there was an actor?
  */
  def upload(cidStr: String, destFolderStr: String) = Action(parse.multipartFormData) { implicit request =>
    // TODO: UserId should be placed as an implicit on the request in the Authenticated action.
    val tmpUserId = UserId.create()
    val status = request.body.files.headOption.map { tmp =>
      FileWrapper(
        filename = tmp.filename,
        contentType = tmp.contentType,
        cid = CustomerId(cidStr),
        folder = Option(Folder(destFolderStr)),
        stream = Option(new FileInputStream(tmp.ref.file))
      )
    }
    status.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>
      logger.debug(s"Going to save file $fw")
      saveFileWrapper(tmpUserId, fw).fold(
        InternalServerError(Json.obj("msg" -> "bad things"))
      )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
    }
  }

  def getFileById(id: String) = Action { implicit request =>
    serve(getFileWrapper(FileId.asId(id)))
  }

}
