package net.scalytica.symbiotic.api.types

import org.slf4j.LoggerFactory

import scala.util.matching.Regex

/**
 * Simulates a folder (directory) in a file system.
 *
 * Folder paths are built up using materialized paths pattern in MongoDB
 * (See http://docs.mongodb.org/manual/tutorial/model-tree-structures-with-materialized-paths)
 *
 * Basically each file will be stored with a path. This path is relevant to the
 * location of the file. The path is stored as a , (comma) separated String. Each
 * customer gets 1 base folder called ,root,.
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
   * Converts the path value into a comma separated (materialized) String for
   * persistence.
   */
  def materialize: String = clean().replaceAll("/", ",")

  def nameOfLast: String = path.split("/").last

  def parent: Path = Path(path.substring(0, path.lastIndexOf("/")))

}

object Path {

  private val logger = LoggerFactory.getLogger(Path.getClass)

  val empty: Path = Path("")

  val root: Path = Path("/root")

  def regex(p: Path, subFoldersOnly: Boolean = false): Regex = {
    val base = s"^${p.materialize}"
    if (subFoldersOnly) (base + "[^,\r\n]*,$").r
    else base.r
  }

  def toDisplay(p: Path): String = Option(p.path).getOrElse("/")

  def fromDisplay(s: Option[String]): Path =
    s.map(Path.apply).getOrElse(root)

}

case class PathNode(
    fid: FileId,
    name: String,
    path: Path,
    children: Seq[PathNode] = Nil
) {

  private val logger = LoggerFactory.getLogger(PathNode.getClass)

  def same(p: Path): Boolean = name == p.nameOfLast && path == p

  def contains(pn: PathNode): Boolean =
    same(pn.path) || children.exists(_.contains(pn))

  def getChild(pn: PathNode): Option[PathNode] =
    if (same(pn.path)) Some(this)
    else if (children.nonEmpty) children.find(_.getChild(pn).isDefined)
    else None

  def add(pn: PathNode): PathNode =
    if (same(pn.path)) this
    else if (same(pn.path.parent)) copy(children = children ++ Seq(pn))
    else copy(children = children.map(_.add(pn)))

}

object PathNode {
  private val logger = LoggerFactory.getLogger(PathNode.getClass)

  val root: PathNode = PathNode(FileId.empty, "root", Path.root)

  def fromPaths(pathItems: Seq[(FileId, Path)]): PathNode = {
    var rootNode = pathItems.headOption.map { fp =>
      PathNode(fp._1, fp._2.nameOfLast, fp._2)
    }.getOrElse(root)

    pathItems.foreach { curr =>
      rootNode = rootNode.add(PathNode(curr._1, curr._2.nameOfLast, curr._2))
    }
    rootNode
  }

}
