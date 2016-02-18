/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.dman

import net.scalytica.symbiotic.core.http.Failed
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.logger._
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case class FolderItem(name: String, path: String, children: Seq[FolderItem]) {

  def parentPath: String = path.substring(0, path.lastIndexOf("/"))

  def contains(item: FolderItem): Boolean =
    (name == item.name && path == item.path) ||
      children.exists(_.contains(item))

  def appendItem(item: FolderItem): FolderItem =
    if (children.nonEmpty) {
      if (contains(item)) this
      else {
        /*
          TODO: I think this is the naive bit that should be more intricate! Possibly
           it is necessary to do some more checks on which child to update!
         */
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

  override def toString = s"fullPath: $path, children:\n ${children.mkString("\n")}"
}

object FolderItem {
  val empty = new FolderItem("", "", Nil)
}

case class FTree(root: FolderItem)

object FTree {
  val rootFolder = "/root/"

  def load: Future[Either[Failed, FTree]] = {
    for {
      xhr <- Ajax.get(
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
          log.debug(items)
          Right(FTree(items))
        case nc: Int if nc == 204 =>
          Left(Failed("No Content"))
        case _ =>
          Left(Failed(xhr.responseText))
      }
    }
  }
}