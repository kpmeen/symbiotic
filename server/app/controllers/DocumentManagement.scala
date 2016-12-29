/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.io.FileInputStream

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Silhouette
import core.security.authentication.JWTEnvironment
import models.docmanagement.Implicits.Defaults._
import models.docmanagement._
import models.party.PartyBaseTypes.UserId
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import services.docmanagement.DocManagementService

@Singleton
class DocumentManagement @Inject() (
    val messagesApi: MessagesApi,
    val silhouette: Silhouette[JWTEnvironment],
    val dmService: DocManagementService
) extends SymbioticController with FileStreaming {

  import silhouette.SecuredAction

  private val log = Logger(this.getClass)

  def getTreePaths(path: Option[String]) = SecuredAction { implicit request =>
    val mp = path.map(Path.apply)
    val folders = dmService.treePaths(mp)(request.identity.id.get).map(_._2)
    if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
  }

  def getFolderHierarchy(path: Option[String]) = SecuredAction { implicit request =>
    val uid = request.identity.id.get
    val folders: Seq[(FileId, Path)] = dmService.treePaths(path.map(Path.apply))(uid)
    if (folders.isEmpty) NoContent
    else Ok(Json.toJson(PathNode.fromPaths(folders)))
  }

  def getRootTree(includeFiles: Boolean = false) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    getTree(None, includeFiles)(currUsrId)
  }

  def getSubTree(path: String, includeFiles: Boolean = false) =
    SecuredAction { implicit request =>
      val currUsrId = request.identity.id.get
      getTree(Option(path), includeFiles)(currUsrId)
    }

  private[this] def getTree(
    path: Option[String],
    includeFiles: Boolean
  )(implicit owner: UserId) = {
    val from = path.map(Path.apply)
    if (includeFiles) {
      val twf = dmService.treeWithFiles(from)
      if (twf.isEmpty) NoContent else Ok(Json.toJson(twf))
    } else {
      val tnf = dmService.treeNoFiles(from)
      if (tnf.isEmpty) NoContent else Ok(Json.toJson(tnf))
    }
  }

  def getDirectDescendantsByPath(path: Option[String]) =
    SecuredAction { implicit request =>
      val currUsrId = request.identity.id.get
      val cwf = dmService.childrenWithFiles(path.map(Path.apply))(currUsrId)
      if (cwf.isEmpty) NoContent else Ok(Json.toJson(cwf))
    }

  def getDirectDescendantsById(folderId: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    dmService.getFolder(folderId)(currUsrId)
      .map { f =>
        val cwf = dmService.childrenWithFiles(f.metadata.path)(currUsrId)
        Ok(Json.obj(
          "folder" -> Json.toJson(f),
          "content" -> Json.toJson(cwf)
        ))
      }
      .getOrElse(NotFound)
  }

  def showFiles(path: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    val lf = dmService.listFiles(Path(path))(currUsrId)
    if (lf.isEmpty) NoContent else Ok(Json.toJson[Seq[File]](lf))
  }

  def lock(fileId: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    // TODO: Improve return types from lockFile to be able to provide better error handling
    dmService.lockFile(fileId)(currUsrId)
      .map(l => Ok(Json.toJson(l)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not lock file $fileId")))
  }

  def unlock(fileId: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    // TODO: Improve return types from unlockFile to be able to provide better error handling
    if (dmService.unlockFile(fileId)(currUsrId)) {
      Ok(Json.obj("msg" -> s"File $fileId is now unlocked"))
    } else {
      BadRequest(Json.obj("msg" -> s"Could not unlock file $fileId"))
    }
  }

  def isLocked(fileId: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    Ok(Json.obj("hasLock" -> dmService.hasLock(fileId)(currUsrId)))
  }

  def addFolderToPath(fullPath: String, createMissing: Boolean = true) =
    SecuredAction { implicit request =>
      val currUsrId = request.identity.id.get
      // TODO: Improve return types from createFolder to be able to provide better error handling
      dmService.createFolder(Path(fullPath), createMissing)(currUsrId)
        .map(fid => Created(Json.toJson(fid)))
        .getOrElse(BadRequest(Json.obj("msg" -> s"Could not create folder at $fullPath")))
    }

  def addFolderToParent(parentId: String, name: String) =
    SecuredAction { implicit request =>
      val currUsrId = request.identity.id.get
      dmService.getFolder(FileId.asId(parentId))(currUsrId).map { f =>
        val currPath = f.flattenPath
        dmService.createFolder(currPath.copy(s"${currPath.path}/$name"))(currUsrId)
          .map(fid => Created(Json.toJson(fid)))
          .getOrElse(
            BadRequest(Json.obj("msg" -> (s"Could not create folder $name at" +
              s" ${f.flattenPath} with parentId $parentId")))
          )
      }.getOrElse(NotFound(Json.obj("msg" -> s"Could not find folder with id $parentId")))
    }

  // TODO: Use FolderId to identify the folder
  def changeFolderName(orig: String, mod: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    val renamed = dmService.moveFolder(Path(orig), Path(mod))(currUsrId)
    if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
  }

  def moveFolderTo(orig: String, mod: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    val moved = dmService.moveFolder(Path(orig), Path(mod))(currUsrId)
    if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
  }

  def moveFileTo(fileId: String, orig: String, dest: String) =
    SecuredAction { implicit request =>
      val currUsrId = request.identity.id.get
      // TODO: Improve return types from moveFile to be able to provide better error handling
      dmService.moveFile(fileId, Path(orig), Path(dest))(currUsrId)
        .map(fw => Ok(Json.toJson(fw)))
        .getOrElse {
          BadRequest(Json.obj("msg" -> (s"Could not move the file with id $fileId " +
            s"from $orig to $dest")))
        }
    }

  def uploadWithPath(destFolderStr: String) =
    SecuredAction(parse.multipartFormData) { implicit request =>
      val f = request.body.files.headOption.map { tmp =>
        File(
          filename = tmp.filename,
          contentType = tmp.contentType,
          metadata = ManagedFileMetadata(
            owner = request.identity.id,
            path = Option(Path(destFolderStr)),
            uploadedBy = request.identity.id
          ),
          stream = Option(new FileInputStream(tmp.ref.file))
        )
      }
      f.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>
        log.debug(s"Going to save file $fw")
        dmService.saveFile(fw)(request.identity.id.get).fold(
          // TODO: This _HAS_ to be improved to be able to return more granular error messages
          InternalServerError(Json.obj("msg" -> "bad things"))
        )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
      }
    }

  def uploadToFolder(folderId: String) =
    SecuredAction(parse.multipartFormData) { implicit request =>
      val currUsrId = request.identity.id.get
      val folder = dmService.getFolder(FileId.asId(folderId))(currUsrId)
      folder.map { fldr =>
        val f = request.body.files.headOption.map { tmp =>
          File(
            filename = tmp.filename,
            contentType = tmp.contentType,
            metadata = ManagedFileMetadata(
              owner = request.identity.id,
              path = fldr.metadata.path,
              uploadedBy = request.identity.id
            ),
            stream = Option(new FileInputStream(tmp.ref.file))
          )
        }
        f.fold(BadRequest(Json.obj("msg" -> "No document attached"))) { fw =>
          log.debug(s"Going to save file $fw")
          dmService.saveFile(fw)(currUsrId).fold(
            // TODO: This _HAS_ to be improved to be able to return more granular error messages
            InternalServerError(Json.obj("msg" -> "bad things"))
          )(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
        }
      }.getOrElse(NotFound(Json.obj("msg" -> s"Could not find the folder $folderId")))
    }

  def getFileById(id: String) = SecuredAction { implicit request =>
    val currUsrId = request.identity.id.get
    serve(dmService.getFile(FileId.asId(id))(currUsrId))
  }

}
