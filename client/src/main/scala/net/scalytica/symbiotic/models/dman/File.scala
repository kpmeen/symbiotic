/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished}
import net.scalytica.symbiotic.routing.SymbioticRouter
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case class File(
  id: String,
  filename: String = "",
  contentType: Option[String] = None,
  uploadDate: Option[String] = None,
  length: Option[Long] = None,
  metadata: FileMetadata) {

  def path = metadata.path.map(_.stripPrefix("/root"))

  def downloadLink = s"${SymbioticRouter.ServerBaseURI}/document/${metadata.fid}"

}

object File {

  def load(oid: String, folder: Option[String]): Future[Either[Failed, Seq[File]]] = {
    val path = folder.map(fp => s"?path=$fp").getOrElse("")
    for {
      xhr <- Ajax.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$oid/folder$path",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Right(read[Seq[File]](xhr.responseText))
      else Left(Failed(xhr.responseText))
    }
  }

  def lock(fileId: String): Future[Either[Failed, Lock]] =
    for {
      xhr <- Ajax.put(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$fileId/lock",
        headers = Map(
          "Accept" -> "application/json"
        )
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Right(read[Lock](xhr.responseText))
      else Left(Failed(xhr.responseText))
    }

  def unlock(fileId: String): Future[AjaxStatus] =
    for {
      xhr <- Ajax.put(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$fileId/unlock",
        headers = Map(
          "Accept" -> "application/json"
        )
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Finished
      else Failed(xhr.responseText)
    }

}
