/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream
import javax.inject.Singleton

import core.security.authentication.Authenticated
import models.docmanagement.Implicits.Defaults._
import models.docmanagement._
import models.party.PartyBaseTypes.OrganisationId
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Controller
import services.docmanagement.Operations

@Singleton
class DocumentManagement extends Controller with Operations with FileStreaming {

  private[this] val logger = Logger(this.getClass)

  private[this] def getTree(oid: OrganisationId, path: Option[String], includeFiles: Boolean) = {
    val from = path.map(Path.apply).getOrElse(Path.root)
    if (includeFiles) {
      val twf = treeWithFiles(oid, from)
      if (twf.isEmpty) NoContent else Ok(Json.toJson(twf))
    } else {
      val tnf = treeNoFiles(oid, from)
      if (tnf.isEmpty) NoContent else Ok(Json.toJson(tnf))
    }
  }

  def getTreePaths(oid: OrganisationId, path: String) = Authenticated { implicit request =>
    val folders = treePaths(oid, Path(path))
    if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
  }

  def getRootTree(customerId: String, includeFiles: Boolean = false) = Authenticated { implicit request =>
    getTree(customerId, None, includeFiles)
  }

  def getSubTree(customerId: String, path: String, includeFiles: Boolean = false) = Authenticated { implicit request =>
    getTree(customerId, Option(path), includeFiles)
  }

  def getDirectDescendants(customerId: String, path: String) = Authenticated { implicit request =>
    val cwf = childrenWithFiles(customerId, Path(path))
    if (cwf.isEmpty) NoContent else Ok(Json.toJson(cwf))
  }

  def showFiles(customerId: String, path: String) = Authenticated { implicit request =>
    val lf = listFiles(customerId, Path(path))
    if (lf.isEmpty) NoContent else Ok(Json.toJson[Seq[File]](lf))
  }

  def lock(fileId: String) = Authenticated { implicit request =>
    val uid = request.user.id.get
    // TODO: Improve return types from lockFile to be able to provide better error handling
    lockFile(uid, fileId)
      .map(l => Ok(Json.toJson(l)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not lock file $fileId")))
  }

  def unlock(fileId: String) = Authenticated { implicit request =>
    val uid = request.user.id.get
    // TODO: Improve return types from unlockFile to be able to provide better error handling
    if (unlockFile(uid, fileId)) Ok(Json.obj("msg" -> s"File $fileId is now unlocked"))
    else BadRequest(Json.obj("msg" -> s"Could not unlock file $fileId"))
  }

  def isLocked(fileId: String) = Authenticated { implicit request =>
    Ok(Json.obj("hasLock" -> hasLock(fileId)))
  }

  def addFolder(customerId: String, fullPath: String, createMissing: Boolean = true) = Authenticated { implicit request =>
    // TODO: Improve return types from createFolder to be able to provide better error handling
    createFolder(customerId, Path(fullPath), createMissing)
      .map(fid => Created(Json.toJson(fid)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not create folder at $fullPath")))
  }

  def changeFolderName(customerId: String, orig: String, mod: String) = Authenticated { implicit request =>
    val renamed = moveFolder(customerId, Path(orig), Path(mod))
    if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
  }

  def moveFolderTo(customerId: String, orig: String, mod: String) = Authenticated { implicit request =>
    val moved = moveFolder(customerId, Path(orig), Path(mod))
    if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
  }

  def moveFileTo(fileId: String, orig: String, dest: String) = Authenticated { implicit request =>
    // TODO: Improve return types from moveFile to be able to provide better error handling
    moveFile(fileId, Path(orig), Path(dest))
      .map(fw => Ok(Json.toJson(fw)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not move the file with id $fileId from $orig to $dest")))
  }

  /*
   TODO: #1 - Create a custom body parser that checks for the existence of the attached file...and handle appropriately.
   TODO: #2 - Integrate with ClammyScan
   TODO: #3 - Evaluate possibility of streaming upload...maybe it will be supported in play 2.4? What if there was an actor?
  */
  def upload(oidStr: String, destFolderStr: String) = Authenticated(parse.multipartFormData) { implicit request =>
    val uid = request.user.id.get
    val status = request.body.files.headOption.map { tmp =>
      File(
        filename = tmp.filename,
        contentType = tmp.contentType,
        metadata = FileMetadata(
          oid = OrganisationId(oidStr),
          path = Option(Path(destFolderStr))
        ),
        stream = Option(new FileInputStream(tmp.ref.file))
      )
    }
    status.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>
      logger.debug(s"Going to save file $fw")
      saveFile(uid, fw).fold(
        InternalServerError(Json.obj("msg" -> "bad things"))
      )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
    }
  }

  def getFileById(id: String) = Authenticated { implicit request =>
    serve(getFile(FileId.asId(id)))
  }

}
