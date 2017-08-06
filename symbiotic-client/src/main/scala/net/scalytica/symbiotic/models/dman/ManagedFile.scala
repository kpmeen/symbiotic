package net.scalytica.symbiotic.models.dman

import japgolly.scalajs.react.Callback
import net.scalytica.symbiotic.core.http.{
  AjaxStatus,
  Failed,
  Finished,
  SymbioticRequest
}
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.routing.SymbioticRouter
import org.scalajs.dom.raw._
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class ManagedFile(
    id: String,
    filename: String = "",
    contentType: Option[String] = None,
    uploadDate: Option[String] = None,
    length: Option[String] = None,
    metadata: FileMetadata
) {

  def fileId = FileId(metadata.fid)

  def path = metadata.path.map(_.stripPrefix("/root"))

  def downloadLink =
    s"${SymbioticRouter.ServerBaseURI}/document/${metadata.fid}"
}

case class ManagedFolder(
    folder: Option[ManagedFile],
    content: Seq[ManagedFile]
)

object ManagedFile {

  implicit val fileFormat: Format[ManagedFile]     = Json.format[ManagedFile]
  implicit val folderFormat: Format[ManagedFolder] = Json.format[ManagedFolder]

  def load(folder: Option[String]): Future[Either[Failed, ManagedFolder]] = {
    val path = folder.map(fp => s"?path=$fp").getOrElse("")
    (for {
      xhr <- SymbioticRequest.get(
              url = s"${SymbioticRouter.ServerBaseURI}/document/folder$path",
              headers = Map(
                "Accept"       -> "application/json",
                "Content-Type" -> "application/json"
              )
            )
    } yield {
      xhr.status match {
        case ok: Int if ok == 200 =>
          log.debug(xhr.responseText)
          Json.fromJson[Seq[ManagedFile]](Json.parse(xhr.responseText)) match {
            case JsSuccess(mfs, p) =>
              Right(ManagedFolder(None, mfs))

            case err: JsError =>
              Left(Failed(Json.stringify(JsError.toJson(err))))
          }
        case nc: Int if nc == 204 =>
          Right(ManagedFolder(None, Seq.empty[ManagedFile]))
        case _ =>
          Left(Failed(xhr.responseText))
      }
    }).recover {
      case err =>
        log.error(err.getMessage)
        Left(Failed(err.getMessage))
    }
  }

  def load(folderId: FileId): Future[Either[Failed, ManagedFolder]] = {
    for {
      xhr <- SymbioticRequest.get(
              url =
                s"${SymbioticRouter.ServerBaseURI}/document/folder/${folderId.value}",
              headers = Map(
                "Accept"       -> "application/json",
                "Content-Type" -> "application/json"
              )
            )
    } yield {
      xhr.status match {
        case ok: Int if ok == 200 =>
          Right(Json.parse(xhr.responseText).as[ManagedFolder])
        case _ =>
          Left(Failed(xhr.responseText))
      }
    }
  }

  def get(mf: ManagedFile): Future[Either[Failed, Blob]] = {
    (for {
      xhr <- SymbioticRequest.get(url = mf.downloadLink, responseType = "blob")
    } yield {
      xhr.status match {
        case ok: Int if ok == 200 =>
          Right(xhr.response.asInstanceOf[Blob])
        case ko =>
          Left(
            Failed(
              s"Status code $ko when trying to download the file ${mf.filename}."
            )
          )
      }
    }).recover {
      case _ =>
        Left(
          Failed(
            s"An error occurred when trying to download the file ${mf.filename}"
          )
        )
    }
  }

  def upload(folderId: Option[FileId], form: HTMLFormElement)(
      callback: Callback
  ) = {
    val url = folderId
      .map(
        fid =>
          s"${SymbioticRouter.ServerBaseURI}/document/folder/${fid.value}/upload"
      )
      .getOrElse(
        s"${SymbioticRouter.ServerBaseURI}/document/upload?path=/root"
      )
    val fd  = new FormData(form)
    val xhr = new XMLHttpRequest
    xhr.onreadystatechange = (e: Event) => {
      if (xhr.readyState == XMLHttpRequest.DONE) {
        if (xhr.status == 200) {
          log.info(xhr.responseText)
          form.reset()
          callback.runNow()
        }
      }
    }
    xhr.open(method = "POST", url = url, async = true)
    Session.token.foreach(
      t => xhr.setRequestHeader(SymbioticRequest.XAuthTokenHeader, t.token)
    )
    xhr.send(fd)
  }

  def lock(fileId: FileId): Future[Either[Failed, Lock]] =
    for {
      xhr <- SymbioticRequest.put(
              url =
                s"${SymbioticRouter.ServerBaseURI}/document/${fileId.value}/lock",
              headers = Map("Accept" -> "application/json")
            )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400)
        Right(Json.parse(xhr.responseText).as[Lock])
      else Left(Failed(xhr.responseText))
    }

  def unlock(fileId: FileId): Future[AjaxStatus] =
    for {
      xhr <- SymbioticRequest.put(
              url =
                s"${SymbioticRouter.ServerBaseURI}/document/${fileId.value}/unlock",
              headers = Map("Accept" -> "application/json")
            )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Finished
      else Failed(xhr.responseText)
    }

  def addFolder(folderId: Option[FileId], name: String) = {
    val addFolderURL = folderId.map { fid =>
      s"${SymbioticRouter.ServerBaseURI}/document/folder/${fid.value}/$name"
    }.getOrElse(
      s"${SymbioticRouter.ServerBaseURI}/document/folder?fullPath=/root/$name"
    )

    for {
      xhr <- SymbioticRequest.post(
              url = addFolderURL,
              headers = Map("Accept" -> "application/json")
            )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Finished
      else Failed(xhr.responseText)
    }
  }

}
