/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import net.scalytica.symbiotic.routes.SymbioticRouter
import net.scalytica.symbiotic.util.Failed
import org.scalajs.dom.ext.Ajax
import upickle._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case class Lock(by: String, date: String)

case class FileWrapper(
  id: String,
  filename: String,
  contentType: Option[String] = None,
  uploadDate: Option[String] = None,
  size: Option[String] = None,
  cid: String,
  pid: Option[String] = None,
  uploadedBy: Option[String] = None,
  version: Int = 1,
  isFolder: Option[Boolean] = None,
  folder: Option[String] = None,
  description: Option[String] = None,
  lock: Option[Lock] = None) {

  def simpleFolderName: String = folder.map(f =>
    f.substring(f.stripSuffix("/").lastIndexOf("/")).stripPrefix("/").stripSuffix("/")
  ).getOrElse("/")

  def path = folder.map(_.stripPrefix("/root"))

  def downloadLink = s"${SymbioticRouter.ServerBaseURI}/document/$id"

}

object FileWrapper {
  def loadF(cid: String, folder: Option[String]): Future[Either[Failed, Seq[FileWrapper]]] = {
    val path = folder.map(fp => s"?path=$fp").getOrElse("")
    for {
      xhr <- Ajax.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$cid/folder$path",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      if (xhr.status >= 200 && xhr.status < 400) Right(read[Seq[FileWrapper]](xhr.responseText))
      else Left(Failed(xhr.responseText))
    }
  }
}