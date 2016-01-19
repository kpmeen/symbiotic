/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import japgolly.scalajs.react.Callback
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished}
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.OrgId
import net.scalytica.symbiotic.routing.SymbioticRouter
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.{Event, FormData, HTMLFormElement, XMLHttpRequest}
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case class ManagedFile(
  id: String,
  filename: String = "",
  contentType: Option[String] = None,
  uploadDate: Option[String] = None,
  length: Option[Long] = None,
  metadata: FileMetadata) {

  def path = metadata.path.map(_.stripPrefix("/root"))

  def downloadLink = s"${SymbioticRouter.ServerBaseURI}/document/${metadata.fid}"

}

object ManagedFile {

  def load(oid: OrgId, folder: Option[String]): Future[Either[Failed, Seq[ManagedFile]]] = {
    val path = folder.map(fp => s"?path=$fp").getOrElse("")
    for {
      xhr <- Ajax.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/${oid.value}/folder$path",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      log.info(xhr.responseText)
      if (xhr.status == 200) Right(read[Seq[ManagedFile]](xhr.responseText))
      else if (xhr.status == 204) Right(Seq.empty[ManagedFile])
      else Left(Failed(xhr.responseText))
    }
  }

  def upload(oid: OrgId, folder: String, form: HTMLFormElement)(callback: Callback) = {
    val url = s"${SymbioticRouter.ServerBaseURI}/document/${oid.value}/upload?path=$folder"
    val fd = new FormData(form)
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
    xhr.send(fd)
  }

  def lock(fileId: String): Future[Either[Failed, Lock]] =
    for {
      xhr <- Ajax.put(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$fileId/lock",
        headers = Map("Accept" -> "application/json")
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Right(read[Lock](xhr.responseText))
      else Left(Failed(xhr.responseText))
    }

  def unlock(fileId: String): Future[AjaxStatus] =
    for {
      xhr <- Ajax.put(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$fileId/unlock",
        headers = Map("Accept" -> "application/json")
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Finished
      else Failed(xhr.responseText)
    }

  def addFolder(oid: OrgId, path: String, name: String) =
    for {
      xhr <- Ajax.post(
        url = s"${SymbioticRouter.ServerBaseURI}/document/${oid.value}/folder?fullPath=$path/$name",
        headers = Map("Accept" -> "application/json")
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Finished
      else Failed(xhr.responseText)
    }

}
