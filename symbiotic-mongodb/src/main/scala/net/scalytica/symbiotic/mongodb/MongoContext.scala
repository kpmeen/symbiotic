package net.scalytica.symbiotic.mongodb

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFS
import com.mongodb.casbah.{
  MongoClient,
  MongoClientURI,
  MongoCollection,
  MongoDB
}
import com.mongodb.gridfs.{GridFS => MongoGridFS}
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.scalytica.symbiotic.mongodb.docmanagement.Indexable
import org.slf4j.LoggerFactory

/**
 * Singleton keeping track of the MongoDB specifics around connectivity etc...
 */
private[mongodb] abstract class MongoContext {
  private[this] val logger = LoggerFactory.getLogger(classOf[MongoContext])
  val dbName: String

  def conf: Config

  lazy val uri = MongoClientURI(
    conf
      .getAs[String]("symbiotic.persistence.mongodb.uri")
      .getOrElse(s"mongodb://localhost:27017")
  )

  def client: MongoClient = {
    logger.debug(s"Intializing mongo client with uri $uri")
    MongoClient(uri)
  }

  def db: MongoDB = client(dbName)
}

private[mongodb] class DefaultContext(val conf: Config) extends MongoContext {
  override val dbName: String =
    conf
      .getAs[String]("symbiotic.persistence.mongodb.dbname.default")
      .getOrElse("symbiotic")
}

private[mongodb] class DManContext(val conf: Config) extends MongoContext {
  override val dbName: String =
    conf
      .getAs[String]("symbiotic.persistence.mongodb.dbname.dman")
      .getOrElse("symbiotic-dman")
}

private[mongodb] sealed trait BaseDB {
  def configuration: Config

  def ctx: MongoContext

  def collectionName: String

  def client: MongoClient = ctx.client

  def db: MongoDB = ctx.db

  lazy val collection: MongoCollection = db(collectionName)
}

/**
 * Trait providing access to a MongoClient, MongoDB and MongoCollection
 */
trait DefaultDB extends BaseDB {
  override def ctx = new DefaultContext(configuration)
}

trait DManDB extends BaseDB {
  override def ctx = new DManContext(configuration)
}

private[mongodb] sealed trait BaseGridFS {
  val bucket: String
  lazy val collectionName: String = s"$bucket.files"
}

trait DefaultGridFS extends BaseGridFS with DefaultDB {
  override val bucket: String = MongoGridFS.DEFAULT_BUCKET
  lazy val gfs: GridFS        = GridFS(db, bucket)
}

trait DManFS extends BaseGridFS with DManDB {
  override val bucket: String = "dman"
  lazy val gfs: GridFS        = GridFS(db, bucket)
}

trait WithMongoIndex {

  private[this] val logger = LoggerFactory.getLogger("Symbiotic")

  def ensureIndex(): Unit

  protected def index(
      keysToindex: Seq[Indexable],
      collection: MongoCollection
  ): Unit = {
    logger.info("Checking indices....")
    val background = DBObject("background" -> true)
    val curr = collection.indexInfo
      .map(_.getAs[MongoDBObject]("key"))
      .filter(_.isDefined)
      .map(_.get.head._1)

    keysToindex.filterNot { k =>
      if (curr.nonEmpty) curr.exists(c => k.keys.contains(c)) else false
    }.foreach {
      case Indexable(keys, unique) =>
        logger.info(
          s"Creating index for $keys in collection ${collection.name}"
        )
        val mdbBldr =
          keys.foldLeft(MongoDBObject.newBuilder)((bldr, k) => bldr += k -> 1)
        collection.createIndex(
          mdbBldr.result(),
          background ++ DBObject("unique" -> unique)
        )
    }
  }

}
