/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import net.scalytica.symbiotic.routes.SymbioticRouter
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

}

object FileWrapper {
  def loadF(cid: String, folder: Option[String]): Future[Seq[FileWrapper]] = {
    val path = folder.map(fp => s"?path=$fp").getOrElse("")
    for {
      json <- Ajax.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$cid/folder$path",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      // The response will be a JSON array of String values.
      read[Seq[FileWrapper]](json.responseText)
    }
  }
}