package controllers

import java.io.FileInputStream

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Silhouette
import core.security.authentication.JWTEnvironment
import models.party.SymbioticUserId
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.core.DocManagementService
import net.scalytica.symbiotic.play.json.Implicits._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.Files
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, MultipartFormData}

@Singleton
class DocumentManagement @Inject()(
    messagesApi: MessagesApi,
    silhouette: Silhouette[JWTEnvironment],
    dmService: DocManagementService
) extends SymbioticController
    with FileStreaming {

  import silhouette.SecuredAction

  private val log = Logger(this.getClass)

  def getTreePaths(path: Option[String]) = SecuredAction { implicit request =>
    implicit val currUsrId = implicitly(request.identity.id.get)
    val mp                 = path.map(Path.apply)
    val folders            = dmService.treePaths(mp).map(_._2)
    if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
  }

  def getFolderHierarchy(path: Option[String]) = SecuredAction {
    implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      val folders: Seq[(FileId, Path)] =
        dmService.treePaths(path.map(Path.apply))
      if (folders.isEmpty) NoContent
      else Ok(Json.toJson(PathNode.fromPaths(folders)))
  }

  def getRootTree(includeFiles: Boolean = false) = SecuredAction {
    implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      getTree(None, includeFiles)
  }

  def getSubTree(path: String, includeFiles: Boolean = false) =
    SecuredAction { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      getTree(Option(path), includeFiles)
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
      implicit val currUsrId = implicitly(request.identity.id.get)
      val cwf                = dmService.childrenWithFiles(path.map(Path.apply))
      if (cwf.isEmpty) NoContent
      else Ok(Json.toJson(cwf))
    }

  def getDirectDescendantsById(folderId: String) = SecuredAction {
    implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      dmService
        .getFolder(folderId)
        .map { f =>
          val cwf = dmService.childrenWithFiles(f.metadata.path)
          Ok(
            Json.obj(
              "folder"  -> Json.toJson(f),
              "content" -> Json.toJson(cwf)
            )
          )
        }
        .getOrElse(NotFound)
  }

  def showFiles(path: String) = SecuredAction { implicit request =>
    implicit val currUsrId = implicitly(request.identity.id.get)

    val lf = dmService.listFiles(Path(path))
    if (lf.isEmpty) NoContent else Ok(Json.toJson[Seq[File]](lf))
  }

  def lock(fileId: String) = SecuredAction { implicit request =>
    implicit val currUsrId = implicitly(request.identity.id.get)
    dmService
      .lockFile(fileId)
      .map(l => Ok(Json.toJson(l)))
      .getOrElse(BadRequest(Json.obj("msg" -> s"Could not lock file $fileId")))
  }

  def unlock(fileId: String) = SecuredAction { implicit request =>
    implicit val currUsrId = implicitly(request.identity.id.get)
    if (dmService.unlockFile(fileId)) {
      Ok(Json.obj("msg" -> s"File $fileId is now unlocked"))
    } else {
      BadRequest(Json.obj("msg" -> s"Could not unlock file $fileId"))
    }
  }

  def isLocked(fileId: String) = SecuredAction { implicit request =>
    implicit val currUsrId = implicitly(request.identity.id.get)
    Ok(Json.obj("hasLock" -> dmService.hasLock(fileId)))
  }

  def addFolderToPath(fullPath: String, createMissing: Boolean = true) =
    SecuredAction { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      dmService
        .createFolder(Path(fullPath), createMissing)
        .map(fid => Created(Json.toJson(fid)))
        .getOrElse(
          BadRequest(
            Json.obj("msg" -> s"Could not create folder at $fullPath")
          )
        )
    }

  def addFolderToParent(parentId: String, name: String) =
    SecuredAction { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      dmService
        .getFolder(FileId.asId(parentId))
        .map { f =>
          val currPath = f.flattenPath
          dmService
            .createFolder(currPath.copy(s"${currPath.path}/$name"))
            .map(fid => Created(Json.toJson(fid)))
            .getOrElse(
              BadRequest(
                Json.obj(
                  "msg" -> (s"Could not create folder $name at" +
                    s" ${f.flattenPath} with parentId $parentId")
                )
              )
            )
        }
        .getOrElse {
          NotFound(
            Json.obj("msg" -> s"Could not find folder with id $parentId")
          )
        }
    }

  // TODO: Use FolderId to identify the folder
  def changeFolderName(orig: String, mod: String) =
    SecuredAction { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      val renamed            = dmService.moveFolder(Path(orig), Path(mod))
      if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
    }

  def moveFolderTo(orig: String, mod: String) =
    SecuredAction { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      val moved              = dmService.moveFolder(Path(orig), Path(mod))
      if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
    }

  def moveFileTo(fileId: String, orig: String, dest: String) =
    SecuredAction { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      dmService
        .moveFile(fileId, Path(orig), Path(dest))
        .map(fw => Ok(Json.toJson(fw)))
        .getOrElse {
          BadRequest(
            Json.obj(
              "msg" -> (s"Could not move the file with id $fileId " +
                s"from $orig to $dest")
            )
          )
        }
    }

  def uploadWithPath(
      destFolderStr: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
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
        dmService
          .saveFile(fw)(request.identity.id.get, SymbioticUserId.asId)
          .map(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
          .getOrElse(InternalServerError(Json.obj("msg" -> "bad things")))
      }
    }

  def uploadToFolder(
      folderId: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    SecuredAction(parse.multipartFormData) { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      val folder             = dmService.getFolder(FileId.asId(folderId))
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
          dmService
            .saveFile(fw)
            .map(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
            .getOrElse(InternalServerError(Json.obj("msg" -> "bad things")))
        }
      }.getOrElse(
        NotFound(Json.obj("msg" -> s"Could not find the folder $folderId"))
      )
    }

  def getFileById(id: String) = SecuredAction { implicit request =>
    implicit val currUsrId = implicitly(request.identity.id.get)
    serve(dmService.getFile(FileId.asId(id)))
  }

}
