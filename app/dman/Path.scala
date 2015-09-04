/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

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
case class Path(
  var path: String = "root") {

  path = path.replaceAll(",", "/")

  /**
   * Converts the path value into a comma separated (materialized) String for persistence.
   */
  def materialize: String = {
    val x = if (!path.startsWith("/")) s"/$path" else path
    val y = if (!x.endsWith("/")) s"$x/" else x
    val z = if (!y.startsWith("/root")) s"/root$y" else y
    z.replaceAll("/", ",")
  }

  def nameOfLast: String = path.split("/").last

}

object Path {

  val logger = LoggerFactory.getLogger(Path.getClass)

  val reads: Reads[Path] = __.readNullable[String].map(fromDisplay)
  val writes: Writes[Path] = Writes {
    case f: Path => JsString(Path.toDisplay(f))
  }

  implicit val format: Format[Path] = Format(reads, writes)

  val root: Path = Path("root")

  def regex(p: Path, subFoldersOnly: Boolean = false): Regex = {
    val base = s"^${p.materialize}"
    if (subFoldersOnly) (base + "[^,\r\n]*,$").r
    else base.r
  }

  private def toDisplay(p: Path): String = Option(p.path).getOrElse("/")

  private def fromDisplay(s: Option[String]): Path = s.map(Path.apply).getOrElse(root)

  //  /**
  //   * Intended for pushing vast amounts of folders into gridfs...
  //   *
  //   * TODO: Move to test sources (?)
  //   */
  //  private[dman] def bulkInsert(cid: CustomerId, fl: List[FolderPath]): Unit = {
  //    val toAdd = fl.map(f => MongoDBObject(MetadataKey -> MongoDBObject(
  //      CidKey.key -> cid.value,
  //      PathKey.key -> f.materialize,
  //      IsFolderKey.key -> true
  //    )))
  //    Try[Unit](collection.insert(toAdd: _*)).recover {
  //      case e: Throwable => logger.error(s"An error occurred inserting a bulk of folders", e)
  //    }
  //  }
}