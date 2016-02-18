/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.matching.Regex

/**
 * Simulates a folder (directory) in a file system.
 *
 * Folder paths are built up using materialized paths pattern in MongoDB
 * (See http://docs.mongodb.org/manual/tutorial/model-tree-structures-with-materialized-paths)
 *
 * Basically each file will be stored with a path. This path is relevant to the location of the file.
 * The path is stored as a , (comma) separated String. Each customer gets 1 base folder called ,root,.
 */
case class Path(var path: String = "/root/") {

  path = path.replaceAll(",", "/")
  path = clean().stripSuffix("/")

  private def clean() = {
    val x = if (!path.startsWith("/")) s"/$path" else path
    val y = if (!x.endsWith("/")) s"$x/" else x
    if (!y.startsWith("/root")) s"/root$y" else y
  }

  /**
   * Converts the path value into a comma separated (materialized) String for persistence.
   */
  def materialize: String = clean().replaceAll("/", ",")

  def nameOfLast: String = path.split("/").last

  def parent: Path = Path(path.substring(0, path.lastIndexOf("/")))

}

object Path {

  val logger = LoggerFactory.getLogger(Path.getClass)

  val reads: Reads[Path] = __.readNullable[String].map(fromDisplay)
  val writes: Writes[Path] = Writes {
    case f: Path => JsString(Path.toDisplay(f))
  }

  implicit val format: Format[Path] = Format(reads, writes)

  val empty: Path = Path("")

  val root: Path = Path("/root")

  def regex(p: Path, subFoldersOnly: Boolean = false): Regex = {
    val base = s"^${p.materialize}"
    if (subFoldersOnly) (base + "[^,\r\n]*,$").r
    else base.r
  }

  private def toDisplay(p: Path): String = Option(p.path).getOrElse("/")

  private def fromDisplay(s: Option[String]): Path = s.map(Path.apply).getOrElse(root)

}

case class PathNode(name: String, path: Path, children: Seq[PathNode] = Nil) {

  val logger = LoggerFactory.getLogger(PathNode.getClass)

  def same(pn: PathNode): Boolean = name == pn.name && path == pn.path

  def contains(pn: PathNode): Boolean = same(pn) || children.exists(_.contains(pn))

  def getChild(pn: PathNode): Option[PathNode] =
    if (same(pn)) Some(this)
    else if (children.nonEmpty) children.find(_.getChild(pn).isDefined)
    else None

  def add(pn: PathNode): PathNode =
    if (same(pn)) this
    else if (same(PathNode.fromPath(pn.path.parent))) copy(children = children ++ Seq(pn))
    else copy(children = children.map(_.add(pn)))

}

object PathNode {
  val logger = LoggerFactory.getLogger(PathNode.getClass)

  implicit val formats = Json.format[PathNode]

  val empty: PathNode = PathNode("", Path.empty)

  def fromPath(p: Path): PathNode = PathNode(p.nameOfLast, p)

  val root: PathNode = PathNode("root", Path.root)

  def fromPaths(pathItems: Seq[Path]): PathNode = {
    var rootNode = root
    pathItems.foreach { curr =>
      rootNode = rootNode.add(fromPath(curr))
    }
    rootNode
  }

}