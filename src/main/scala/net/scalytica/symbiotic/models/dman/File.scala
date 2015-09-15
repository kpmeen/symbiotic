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

case class File(
  id: String,
  filename: String,
  contentType: Option[String] = None,
  uploadDate: Option[String] = None,
  size: Option[String] = None,
  metadata: FileMetadata) {

  def simpleFolderName: String = metadata.path.map(f =>
    f.substring(f.stripSuffix("/").lastIndexOf("/")).stripPrefix("/").stripSuffix("/")
  ).getOrElse("/")

  def path = metadata.path.map(_.stripPrefix("/root"))

  def downloadLink = s"${SymbioticRouter.ServerBaseURI}/document/$id"

}

object File {
  def loadF(oid: String, folder: Option[String]): Future[Either[Failed, Seq[File]]] = {
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
}