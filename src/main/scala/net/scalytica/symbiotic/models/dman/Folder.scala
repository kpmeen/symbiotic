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

case class FolderItem(cid: String, folderName: String, fullPath: String, children: List[FolderItem]) {
  def contains(fname: String): Boolean = (folderName == fname) || children.exists(_.contains(fname))

  def appendItem(item: FolderItem): FolderItem =
    if (children.nonEmpty) copy(children = List(children.head.appendItem(item)))
    else copy(children = List(item))

  private[dman] def lastChild: FolderItem =
    if (children.nonEmpty) children.head.lastChild
    else this
}

object FolderItem {
  val empty = new FolderItem("", "", "", Nil)
}

case class FTree(folders: Seq[FolderItem])

object FTree {
  val rootFolder = "/root/"

  def loadF(cid: String): Future[Either[Failed, FTree]] = {
    for {
      xhr <- Ajax.get(
        url = s"${SymbioticRouter.ServerBaseURI}/document/$cid/tree",
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      // The response will be a JSON array of String values.
      if (xhr.status >= 200 && xhr.status < 400) Right(FTree.fromFolderList(cid, read[Seq[String]](xhr.responseText)))
      else Left(Failed(xhr.responseText))
    }
  }

  def fromFolderList(cid: String, fSeq: Seq[String]): FTree =
    FTree(
      fSeq.tail.reverse.foldLeft(List.empty[String]) {
        case (prev: List[String], f: String) =>
          if (prev.exists(_.startsWith(f))) prev
          else prev ::: List(f)
      }.map { args =>
        val clean = args.stripPrefix(rootFolder).stripSuffix("/").split("/").toList
        clean.foldLeft[FolderItem](FolderItem.empty) { (item: FolderItem, curr: String) =>
          if (FolderItem.empty == item) {
            FolderItem(cid, folderName = curr, fullPath = s"/$curr", children = Nil)
          } else {
            val fi = FolderItem(cid, folderName = curr, fullPath = s"${item.lastChild.fullPath}/$curr", children = Nil)
            item.appendItem(fi)
          }
        }
      }
    )
}