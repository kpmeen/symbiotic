/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import net.scalytica.symbiotic.core.http.{SymbioticRequest, Failed}
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.routing.SymbioticRouter
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class FolderItem(fid: String, name: String, path: String, children: Seq[FolderItem]) {

  def folderId: FileId = fid

  def parentPath: String = path.substring(0, path.lastIndexOf("/"))

  def has(item: FolderItem): Boolean =
    (name == item.name && path == item.path) || children.exists(_.has(item))

  def has(fldrId: FileId): Boolean =
    folderId == fldrId || children.exists(_.has(fldrId))


  def buildPathLink(fldrId: FileId): Seq[(FileId, String)] = {
    val pls = Seq.newBuilder[(FileId, String)]
    if (folderId == fldrId) pls += ((folderId, name))
    else {
      val idxc = children.zipWithIndex.filter(_._1.has(fldrId))
      if (idxc.nonEmpty && idxc.size == 1)
        pls += ((folderId, name))
        pls ++= idxc.head._1.buildPathLink(fldrId)
    }
    pls.result()
  }

  def appendItem(item: FolderItem): FolderItem =
    if (children.nonEmpty) {
      if (has(item)) this
      else {
        val zipped = children.zipWithIndex
        val maybeChild = zipped.find(c => item.path.contains(c._1.path))
        maybeChild.map {
          case (fi: FolderItem, i: Int) =>
            copy(children = children.updated(i, fi.appendItem(item)))
        }.getOrElse(this)
      }
    }
    else {
      copy(children = Seq(item))
    }
}

object FolderItem {
  val empty = new FolderItem("", "", "", Nil)
}

case class FTree(root: FolderItem)

object FTree {
  val rootFolder = "/root/"

  def load: Future[Either[Failed, FTree]] = {
    for {
      xhr <- SymbioticRequest.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/tree/hierarchy",
        //        url = s"${SymbioticRouter.ServerBaseURI}/document/tree/paths",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      // The response will be a JSON array of String values.
      xhr.status match {
        case ok: Int if ok == 200 =>
          val items = read[FolderItem](xhr.responseText)
          Right(FTree(items))
        case nc: Int if nc == 204 =>
          Left(Failed("No Content"))
        case _ =>
          Left(Failed(xhr.responseText))
      }
    }
  }
}