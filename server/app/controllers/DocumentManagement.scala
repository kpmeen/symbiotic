/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream
import javax.inject.Singleton

import com.google.inject.Inject
import core.security.authentication.Authenticated
import models.docmanagement.Implicits.Defaults._
import models.docmanagement._
import models.party.PartyBaseTypes.UserId
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Controller
import services.docmanagement.DocManagementService

@Singleton
class DocumentManagement @Inject() (
    val dmService: DocManagementService
) extends Controller with FileStreaming {

  private[this] val logger = Logger(this.getClass)

  private[this] def getTree(path: Option[String], includeFiles: Boolean)(implicit owner: UserId) = {
    val from = path.map(Path.apply)
    if (includeFiles) {
      val twf = dmService.treeWithFiles(from)
      if (twf.isEmpty) NoContent else Ok(Json.toJson(twf))
    } else {
      val tnf = dmService.treeNoFiles(from)
      if (tnf.isEmpty) NoContent else Ok(Json.toJson(tnf))
    }
  }

  def getTreePaths(path: Option[String]) = Authenticated { implicit request =>
    val folders: Seq[Path] = dmService.treePaths(path.map(Path.apply))(request.currentUserId).map(_._2)
    if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
  }

  def getFolderHierarchy(path: Option[String]) = Authenticated { implicit request =>
    val folders: Seq[(FileId, Path)] = dmService.treePaths(path.map(Path.apply))(request.currentUserId)
    if (folders.isEmpty) NoContent
    else Ok(Json.toJson(PathNode.fromPaths(folders)))
  }

  def getRootTree(includeFiles: Boolean = false) = Authenticated { implicit request =>
    getTree(None, includeFiles)(request.currentUserId)
  }

  def getSubTree(path: String, includeFiles: Boolean = false) = Authenticated { implicit request =>
    getTree(Option(path), includeFiles)(request.currentUserId)
  }

  def getDirectDescendantsByPath(path: Option[String]) = Authenticated { implicit request =>
    val cwf = dmService.childrenWithFiles(path.map(Path.apply))(request.currentUserId)
    if (cwf.isEmpty) NoContent else Ok(Json.toJson(cwf))
  }

  def getDirectDescendantsById(folderId: String) = Authenticated { implicit request =>
    dmService.getFolder(folderId)(request.currentUserId)
      .map { f =>
        val cwf = dmService.childrenWithFiles(f.metadata.path)(request.currentUserId)
        Ok(Json.obj(
          "folder" -> Json.toJson(f),
          "content" -> Json.toJson(cwf)
        ))
      }
      .getOrElse(NotFound)
  }

  def showFiles(path: String) = Authenticated { implicit request =>
    val lf = dmService.listFiles(Path(path))(request.currentUserId)
    if (lf.isEmpty) NoContent else Ok(Json.toJson[Seq[File]](lf))
  }

  def lock(fileId: String) = Authenticated { implicit request =>
    // TODO: Improve return types from lockFile to be able to provide better error handling
    dmService.lockFile(fileId)(request.currentUserId)
      .map(l => Ok(Json.toJson(l)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not lock file $fileId")))
  }

  def unlock(fileId: String) = Authenticated { implicit request =>
    // TODO: Improve return types from unlockFile to be able to provide better error handling
    if (dmService.unlockFile(fileId)(request.currentUserId))
      Ok(Json.obj("msg" -> s"File $fileId is now unlocked"))
    else
      BadRequest(Json.obj("msg" -> s"Could not unlock file $fileId"))
  }

  def isLocked(fileId: String) = Authenticated { implicit request =>
    Ok(Json.obj("hasLock" -> dmService.hasLock(fileId)(request.currentUserId)))
  }

  def addFolderToPath(fullPath: String, createMissing: Boolean = true) = Authenticated { implicit request =>
    // TODO: Improve return types from createFolder to be able to provide better error handling
    dmService.createFolder(Path(fullPath), createMissing)(request.currentUserId)
      .map(fid => Created(Json.toJson(fid)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not create folder at $fullPath")))
  }

  def addFolderToParent(parentId: String, name: String) = Authenticated { implicit request =>
    dmService.getFolder(FileId.asId(parentId))(request.currentUserId).map { f =>
      val currPath = f.flattenPath
      dmService.createFolder(currPath.copy(s"${currPath.path}/$name"))(request.currentUserId)
        .map(fid => Created(Json.toJson(fid)))
        .getOrElse(
          BadRequest(Json.obj("msg" -> s"Could not create folder $name at ${f.flattenPath} with parentId $parentId"))
        )
    }.getOrElse(NotFound(Json.obj("msg" -> s"Could not find folder with id $parentId")))
  }

  // TODO: Use FolderId to identify the folder
  def changeFolderName(orig: String, mod: String) = Authenticated { implicit request =>
    val renamed = dmService.moveFolder(Path(orig), Path(mod))(request.currentUserId)
    if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
  }

  def moveFolderTo(orig: String, mod: String) = Authenticated { implicit request =>
    val moved = dmService.moveFolder(Path(orig), Path(mod))(request.currentUserId)
    if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
  }

  def moveFileTo(fileId: String, orig: String, dest: String) = Authenticated { implicit request =>
    // TODO: Improve return types from moveFile to be able to provide better error handling
    dmService.moveFile(fileId, Path(orig), Path(dest))(request.currentUserId)
      .map(fw => Ok(Json.toJson(fw)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not move the file with id $fileId from $orig to $dest")))
  }

  def uploadWithPath(destFolderStr: String) = Authenticated(parse.multipartFormData) { implicit request =>
    val f = request.body.files.headOption.map { tmp =>
      File(
        filename = tmp.filename,
        contentType = tmp.contentType,
        metadata = ManagedFileMetadata(
          owner = request.currentUser.id,
          path = Option(Path(destFolderStr)),
          uploadedBy = request.currentUser.id
        ),
        stream = Option(new FileInputStream(tmp.ref.file))
      )
    }
    f.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>
      logger.debug(s"Going to save file $fw")
      dmService.saveFile(fw)(request.currentUserId).fold(
        // TODO: This _HAS_ to be improved to be able to return more granular error messages
        InternalServerError(Json.obj("msg" -> "bad things"))
      )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
    }
  }

  def uploadToFolder(folderId: String) = Authenticated(parse.multipartFormData) { implicit request =>
    val folder = dmService.getFolder(FileId.asId(folderId))(request.currentUserId)
    folder.map { fldr =>
      val f = request.body.files.headOption.map { tmp =>
        File(
          filename = tmp.filename,
          contentType = tmp.contentType,
          metadata = ManagedFileMetadata(
            owner = request.currentUser.id,
            path = fldr.metadata.path,
            uploadedBy = request.currentUser.id
          ),
          stream = Option(new FileInputStream(tmp.ref.file))
        )
      }
      f.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>
        logger.debug(s"Going to save file $fw")
        dmService.saveFile(fw)(request.currentUserId).fold(
          // TODO: This _HAS_ to be improved to be able to return more granular error messages
          InternalServerError(Json.obj("msg" -> "bad things"))
        )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
      }
    }.getOrElse(NotFound(Json.obj("msg" -> s"Could not find the folder $folderId")))
  }

  def getFileById(id: String) = Authenticated { implicit request =>
    serve(dmService.getFile(FileId.asId(id))(request.currentUserId))
  }

}
