package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Silhouette
import core.security.authentication.JWTEnvironment
import models.base.SymbioticUserId._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.core.DocManagementService
import net.scalytica.symbiotic.json.Implicits._
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, MultipartFormData}

import scala.concurrent.Future

@Singleton
class DocumentManagement @Inject()(
    val controllerComponents: ControllerComponents,
    actorSystem: ActorSystem,
    materializer: Materializer,
    silhouette: Silhouette[JWTEnvironment],
    dmService: DocManagementService
) extends SymbioticController
    with FileStreaming {

  import silhouette.SecuredAction

  private val log = Logger(this.getClass)

  def getTreePaths(path: Option[String]) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)

      val mp = path.map(Path.apply)
      dmService.treePaths(mp).map(_.map(_._2)).map { folders =>
        if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
      }
    }

  def getFolderHierarchy(path: Option[String]) = SecuredAction.async {
    implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)
      dmService.treePaths(path.map(Path.apply)).map { folders =>
        if (folders.isEmpty) NoContent
        else Ok(Json.toJson(PathNode.fromPaths(folders)))
      }
  }

  def getRootTree(includeFiles: Boolean = false) = SecuredAction.async {
    implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)
      getTree(None, includeFiles)
  }

  def getSubTree(path: String, includeFiles: Boolean = false) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId = implicitly(request.identity.id.get)
      getTree(Option(path), includeFiles)
    }

  private[this] def getTree(
      path: Option[String],
      includeFiles: Boolean
  )(implicit owner: UserId) = {
    val from = path.map(Path.apply)
    if (includeFiles) {
      dmService.treeWithFiles(from).map { twf =>
        if (twf.isEmpty) NoContent else Ok(Json.toJson(twf))
      }
    } else {
      dmService.treeNoFiles(from).map { tnf =>
        if (tnf.isEmpty) NoContent else Ok(Json.toJson(tnf))
      }
    }
  }

  def getDirectDescendantsByPath(path: Option[String]) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)

      dmService.childrenWithFiles(path.map(Path.apply)).map { cwf =>
        if (cwf.isEmpty) NoContent
        else Ok(Json.toJson(cwf))
      }
    }

  def getDirectDescendantsById(folderId: String) = SecuredAction.async {
    implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)
      dmService.folder(folderId).flatMap { maybeFolder =>
        maybeFolder.map { f =>
          dmService.childrenWithFiles(f.metadata.path).map { cwf =>
            Ok(
              Json.obj(
                "folder"  -> Json.toJson(f),
                "content" -> Json.toJson(cwf)
              )
            )
          }
        }.getOrElse(Future.successful(NotFound))
      }
  }

  def showFiles(path: String) = SecuredAction.async { implicit request =>
    implicit val currUsrId: UserId = implicitly(request.identity.id.get)

    dmService.listFiles(Path(path)).map { lf =>
      if (lf.isEmpty) NoContent else Ok(Json.toJson[Seq[File]](lf))
    }
  }

  def lock(fileId: String) = SecuredAction.async { implicit request =>
    implicit val currUsrId: UserId = implicitly(request.identity.id.get)
    dmService
      .lockFile(fileId)
      .map(_.map(l => Ok(Json.toJson(l))).getOrElse {
        BadRequest(Json.obj("msg" -> s"Could not lock file $fileId"))
      })
  }

  def unlock(fileId: String) = SecuredAction.async { implicit request =>
    implicit val currUsrId: UserId = implicitly(request.identity.id.get)
    dmService.unlockFile(fileId).map { unlocked =>
      if (unlocked) {
        Ok(Json.obj("msg" -> s"File $fileId is now unlocked"))
      } else {
        BadRequest(Json.obj("msg" -> s"Could not unlock file $fileId"))
      }
    }
  }

  def isLocked(fileId: String) = SecuredAction.async { implicit request =>
    implicit val currUsrId: UserId = implicitly(request.identity.id.get)
    dmService.hasLock(fileId).map { locked =>
      Ok(Json.obj("hasLock" -> locked))
    }
  }

  def addFolderToPath(fullPath: String, createMissing: Boolean = true) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)
      dmService.createFolder(Path(fullPath), createMissing).map { maybeFid =>
        maybeFid
          .map(fid => Created(Json.toJson(fid)))
          .getOrElse(
            BadRequest(
              Json.obj("msg" -> s"Could not create folder at $fullPath")
            )
          )
      }
    }

  def addFolderToParent(parentId: String, name: String) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)
      dmService.folder(FileId.asId(parentId)).flatMap { maybeFolder =>
        maybeFolder.map { f =>
          val currPath = f.flattenPath
          dmService
            .createFolder(currPath.copy(s"${currPath.path}/$name"))
            .map { maybeFid =>
              maybeFid
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
        }.getOrElse {
          Future.successful(
            NotFound(
              Json.obj("msg" -> s"Could not find folder with id $parentId")
            )
          )
        }
      }
    }

  // TODO: Use FolderId to identify the folder
  def changeFolderName(orig: String, mod: String) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)

      dmService.moveFolder(Path(orig), Path(mod)).map { renamed =>
        if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
      }
    }

  def moveFolderTo(orig: String, mod: String) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)

      dmService.moveFolder(Path(orig), Path(mod)).map { moved =>
        if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
      }
    }

  def moveFileTo(fileId: String, orig: String, dest: String) =
    SecuredAction.async { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)
      dmService.moveFile(fileId, Path(orig), Path(dest)).map { mr =>
        mr.map(fw => Ok(Json.toJson(fw))).getOrElse {
          BadRequest(
            Json.obj(
              "msg" -> (s"Could not move the file with id $fileId " +
                s"from $orig to $dest")
            )
          )
        }
      }
    }

  def uploadWithPath(
      destFolderStr: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    SecuredAction.async(parse.multipartFormData) { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)

      val f = request.body.files.headOption.map { tmp =>
        File(
          filename = tmp.filename,
          contentType = tmp.contentType,
          metadata = ManagedMetadata(
            owner = request.identity.id,
            path = Option(Path(destFolderStr)),
            uploadedBy = request.identity.id
          ),
          stream = Option(FileIO.fromPath(tmp.ref.path))
        )
      }

      f.map { fw =>
        log.debug(s"Going to save file $fw")
        dmService.saveFile(fw).map { maybeFid =>
          maybeFid
            .map(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
            .getOrElse(InternalServerError(Json.obj("msg" -> "bad things")))
        }
      }.getOrElse {
        Future.successful(
          BadRequest(Json.obj("msg" -> "No document attached"))
        )
      }
    }

  def uploadToFolder(
      folderId: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    SecuredAction.async(parse.multipartFormData) { implicit request =>
      implicit val currUsrId: UserId = implicitly(request.identity.id.get)

      dmService.folder(FileId.asId(folderId)).flatMap { folder =>
        folder.map { fldr =>
          val f = request.body.files.headOption.map { tmp =>
            File(
              filename = tmp.filename,
              contentType = tmp.contentType,
              metadata = ManagedMetadata(
                owner = request.identity.id,
                path = fldr.metadata.path,
                uploadedBy = request.identity.id
              ),
              stream = Option(FileIO.fromPath(tmp.ref.path))
            )
          }
          f.map { fw =>
            log.debug(s"Going to save file $fw")
            dmService.saveFile(fw).map { maybeFid =>
              maybeFid
                .map(fid => Ok(Json.obj("msg" -> s"Saved file with Id $fid")))
                .getOrElse(
                  InternalServerError(Json.obj("msg" -> "bad things"))
                )
            }
          }.getOrElse {
            Future.successful(
              BadRequest(Json.obj("msg" -> "No document attached"))
            )
          }
        }.getOrElse {
          Future.successful(
            NotFound(Json.obj("msg" -> s"Could not find the folder $folderId"))
          )
        }
      }
    }

  def getFileById(id: String) = SecuredAction.async { implicit request =>
    implicit val currUsrId: UserId = implicitly(request.identity.id.get)
    dmService.file(FileId.asId(id)).map(f => serve(f))
  }

}
