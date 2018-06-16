package controllers

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Silhouette
import core.DocManContext
import core.security.authentication.JWTEnvironment
import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.core.DocManagementService
import net.scalytica.symbiotic.json.Implicits._
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

/*

  TODO:
  This server currently only allows _users_ to be owners of a folder hierarchy.
  If and when time permits, the implementation should be modified to show how
  one would implement a system that allows for orgs to be owners. Including how
  users then can become members of an org and start interacting with the
  folder structure.

 */
@Singleton
class DocumentManagement @Inject()(
    val controllerComponents: ControllerComponents,
    silhouette: Silhouette[JWTEnvironment],
    dmService: DocManagementService
) extends SymbioticController
    with FileStreaming {

  // scalastyle:off public.methods.have.type

  import silhouette.SecuredAction

  private[this] def handleSymRes[A](res: SymRes[A])(ok: A => Result): Result = {
    res match {
      case good: Ok[A]                => ok(good.value)
      case _: NotFound                => NotFound
      case _: NotModified             => NotModified
      case NotEditable(msg)           => BadRequest(Json.obj("message" -> msg))
      case IllegalDestination(msg, _) => BadRequest(Json.obj("message" -> msg))
      case InvalidData(msg)           => BadRequest(Json.obj("message" -> msg))
      case ResourceLocked(msg, _)     => BadRequest(Json.obj("message" -> msg))
      case _: NotLocked =>
        BadRequest(
          Json.obj("message" -> "Resource must be locked before modifying it.")
        )

      case Failed(msg) =>
        InternalServerError(Json.obj("message" -> msg))

    }
  }

  private val log = Logger(this.getClass)

  def getTreePaths(path: Option[String]) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      val mp = path.map(Path.apply)
      dmService.treePaths(mp).map { res =>
        handleSymRes(res) { folders =>
          if (folders.isEmpty) NoContent else Ok(Json.toJson(folders))
        }
      }
    }

  def getFolderHierarchy(path: Option[String]) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.treePaths(path.map(Path.apply)).map { res =>
        handleSymRes(res) { folders =>
          if (folders.isEmpty) NoContent
          else Ok(Json.toJson(PathNode.fromPaths(folders)))
        }
      }
    }

  def getRootTree(includeFiles: Boolean = false) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      getTree(None, includeFiles)
    }

  def getSubTree(path: String, includeFiles: Boolean = false) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      getTree(Option(path), includeFiles)
    }

  private[this] def getTree(
      path: Option[String],
      includeFiles: Boolean
  )(implicit ctx: DocManContext) = {
    val from = path.map(Path.apply)
    if (includeFiles) {
      dmService.treeWithFiles(from).map { res =>
        handleSymRes(res) { twf =>
          if (twf.isEmpty) NoContent else Ok(Json.toJson(twf))
        }
      }
    } else {
      dmService.treeNoFiles(from).map { res =>
        handleSymRes(res) { tnf =>
          if (tnf.isEmpty) NoContent else Ok(Json.toJson(tnf))
        }
      }
    }
  }

  def getDirectDescendantsByPath(path: Option[String]) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.childrenWithFiles(path.map(Path.apply)).map { res =>
        handleSymRes(res) { cwf =>
          if (cwf.isEmpty) NoContent
          else Ok(Json.toJson(cwf))
        }
      }
    }

  def getDirectDescendantsById(folderId: String) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.folder(folderId).flatMap { maybeFolder =>
        maybeFolder.map { f =>
          dmService.childrenWithFiles(f.metadata.path).map { res =>
            handleSymRes(res) { cwf =>
              Ok(
                Json.obj(
                  "folder"  -> Json.toJson(f),
                  "content" -> Json.toJson(cwf)
                )
              )
            }
          }
        }.getOrElse(Future.successful(NotFound))
      }
    }

  def showFiles(path: String) = SecuredAction.async { implicit request =>
    implicit val ctx = DocManContext(request.identity.id.get)

    dmService.listFiles(Path(path)).map { res =>
      handleSymRes(res) { lf =>
        if (lf.isEmpty) NoContent else Ok(Json.toJson[Seq[File]](lf))
      }
    }
  }

  def lock(fileId: String) = SecuredAction.async { implicit request =>
    implicit val ctx = DocManContext(request.identity.id.get)

    dmService
      .lockFile(fileId)
      .map(res => handleSymRes(res)(l => Ok(Json.toJson(l))))
  }

  def unlock(fileId: String) = SecuredAction.async { implicit request =>
    implicit val ctx = DocManContext(request.identity.id.get)

    dmService.unlockFile(fileId).map { res =>
      handleSymRes(res)(_ => Ok(Json.obj("msg" -> s"File $fileId unlocked")))
    }
  }

  def isLocked(fileId: String) = SecuredAction.async { implicit request =>
    implicit val ctx = DocManContext(request.identity.id.get)

    dmService
      .fileHasLock(fileId)
      .map(locked => Ok(Json.obj("hasLock" -> locked)))
  }

  def addFolderToPath(fullPath: String, createMissing: Boolean = true) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.createFolder(Path(fullPath), createMissing).map { res =>
        handleSymRes(res) { fid =>
          Created(Json.obj("fid" -> Json.toJson(fid)))
        }
      }
    }

  def addFolderToParent(parentId: String, name: String) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.folder(FileId.asId(parentId)).flatMap { folderRes =>
        folderRes.map { f =>
          val currPath = f.flattenPath
          dmService
            .createFolder(currPath.copy(s"${currPath.value}/$name"))
            .map { res =>
              handleSymRes(res) { fid =>
                Created(Json.obj("fid" -> Json.toJson(fid)))
              }
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
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.moveFolder(Path(orig), Path(mod)).map { res =>
        handleSymRes(res) { renamed =>
          if (renamed.isEmpty) NoContent else Ok(Json.toJson(renamed))
        }
      }
    }

  def moveFolderTo(orig: String, mod: String) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.moveFolder(Path(orig), Path(mod)).map { res =>
        handleSymRes(res) { moved =>
          if (moved.isEmpty) NoContent else Ok(Json.toJson(moved))
        }
      }
    }

  def moveFileTo(fileId: String, orig: String, dest: String) =
    SecuredAction.async { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.moveFile(fileId, Path(orig), Path(dest)).map { res =>
        handleSymRes(res)(fw => Ok(Json.toJson(fw)))
      }
    }

  def uploadWithPath(
      destFolderStr: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    SecuredAction.async(parse.multipartFormData) { implicit request =>
      implicit val ctx = DocManContext(request.identity.id.get)

      val f = request.body.files.headOption.map { tmp =>
        File(
          filename = tmp.filename,
          fileType = tmp.contentType,
          metadata = ManagedMetadata(
            owner = Option(ctx.owner),
            path = Option(Path(destFolderStr)),
            createdBy = request.identity.id
          ),
          stream = Option(FileIO.fromPath(tmp.ref.path))
        )
      }

      f.map { fw =>
        log.debug(s"Going to save file $fw")
        dmService.saveFile(fw).map { res =>
          handleSymRes(res)(fid => Ok(Json.obj("fid" -> Json.toJson(fid))))
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
      implicit val ctx = DocManContext(request.identity.id.get)

      dmService.folder(FileId.asId(folderId)).flatMap { folder =>
        folder.map { fldr =>
          val f = request.body.files.headOption.map { tmp =>
            File(
              filename = tmp.filename,
              fileType = tmp.contentType,
              metadata = ManagedMetadata(
                owner = Option(ctx.owner),
                path = fldr.metadata.path,
                createdBy = request.identity.id
              ),
              stream = Option(FileIO.fromPath(tmp.ref.path))
            )
          }
          f.map { fw =>
            log.debug(s"Going to save file $fw")
            dmService.saveFile(fw).map { res =>
              handleSymRes(res)(fid => Ok(Json.obj("fid" -> Json.toJson(fid))))
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
    implicit val ctx = DocManContext(request.identity.id.get)

    dmService.file(FileId.asId(id)).map { res =>
      handleSymRes(res)(f => serve(f))
    }
  }

}
