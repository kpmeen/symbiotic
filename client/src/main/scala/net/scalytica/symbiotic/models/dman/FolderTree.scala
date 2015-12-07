/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.core.http.Failed
import net.scalytica.symbiotic.logger.log
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case class FolderItem(oid: String, folderName: String, fullPath: String, children: List[FolderItem]) {
  def contains(fname: String): Boolean = (folderName == fname) || children.exists(_.contains(fname))

  def appendItem(item: FolderItem): FolderItem =
    if (children.nonEmpty) copy(children = List(children.head.appendItem(item)))
    else copy(children = List(item))

  def lastChild: FolderItem =
    if (children.nonEmpty) children.head.lastChild
    else this

  override def toString = s"fullPath: $fullPath, children:\n ${children.mkString("\n")}"
}

object FolderItem {
  val empty = new FolderItem("", "", "", Nil)
}

case class FTree(folders: Seq[FolderItem])

object FTree {
  val rootFolder = "/root/"

  private def root(oid: String, folders: List[FolderItem]) =
    new FolderItem(oid, "root", rootFolder, folders)

  def load(oid: String): Future[Either[Failed, FTree]] = {
    for {
      xhr <- Ajax.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$oid/tree/paths",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      // The response will be a JSON array of String values.
      if (xhr.status >= 200 && xhr.status < 400) Right(FTree.fromFolderList(oid, read[Seq[String]](xhr.responseText)))
      else Left(Failed(xhr.responseText))
    }
  }

  // TODO: Revisit this function...doesn't seem to be correctly processing the data!
  def fromFolderList(oid: String, fSeq: Seq[String]): FTree = {
    val tmp = fSeq.tail.reverse.foldLeft(List.empty[String]) {
      case (prev: List[String], f: String) =>
        if (prev.exists(_.startsWith(f))) prev
        else prev ::: List(f)
    }
    val children = tmp.map { args =>
      val clean = args.stripPrefix(rootFolder).stripSuffix("/").split("/").toList
      clean.foldLeft[FolderItem](FolderItem.empty) { (item: FolderItem, curr: String) =>
        if (FolderItem.empty == item) {
          FolderItem(oid, folderName = curr, fullPath = s"/$curr", children = Nil)
        } else {
          val fi = FolderItem(oid, folderName = curr, fullPath = s"${item.lastChild.fullPath}/$curr", children = Nil)
          item.appendItem(fi)
        }
      }
    }
    FTree(Seq(root(oid, children)))
  }
}