/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.mongodb

import com.mongodb.casbah.gridfs.GridFS
import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection, MongoDB}
import com.mongodb.gridfs.{GridFS => MongoGridFS}
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.Play.maybeApplication

/**
 * Singleton keeping track of the MongoDB specifics around connectivity etc...
 */
private[mongodb] abstract class MongoContext {
  val dbName: String

  val conf = maybeApplication.map(_.configuration).getOrElse(Configuration(ConfigFactory.load()))

  lazy val uri = MongoClientURI(conf.getString("symbiotic.mongodb.uri").getOrElse(s"mongodb://localhost:27017"))

  def client: MongoClient = MongoClient(uri)

  def db: MongoDB = client(dbName)
}

private[mongodb] class DefaultContext extends MongoContext {
  override val dbName: String = conf.getString("symbiotic.mongodb.dbname.default").getOrElse("symbiotic")
}

private[mongodb] class HIPEContext extends MongoContext {
  override val dbName: String = conf.getString("symbiotic.mongodb.dbname.hipe").getOrElse("symbiotic-hipe")
}

private[mongodb] class DManContext extends MongoContext {
  override val dbName: String = conf.getString("symbiotic.mongodb.dbname.dman").getOrElse("symbiotic-dman")
}

private[mongodb] sealed trait BaseDB {
  val ctx: MongoContext
  val collectionName: String

  def client: MongoClient = ctx.client

  def db: MongoDB = ctx.db

  lazy val collection: MongoCollection = db(collectionName)
}

/**
 * Trait providing access to a MongoClient, MongoDB and MongoCollection
 */
trait DefaultDB extends BaseDB {
  override val ctx = new DefaultContext
}

trait HipeDB extends BaseDB {
  override val ctx = new HIPEContext
}

trait DManDB extends BaseDB {
  override val ctx = new DManContext
}

private[mongodb] sealed trait BaseGridFS {
  val bucket: String
  lazy val collectionName: String = s"$bucket.files"
}

trait DefaultGridFS extends BaseGridFS with DefaultDB {
  override val bucket: String = MongoGridFS.DEFAULT_BUCKET
  lazy val gfs: GridFS = GridFS(db, bucket)
}

/**
 * As DmanDB but additionally provides access to GridFS.
 */
trait DManFS extends BaseGridFS with DManDB {
  override val bucket: String = "dman"
  lazy val gfs: GridFS = GridFS(db, bucket)
}

/**
 * Ensure index...
 */
trait WithMongoIndex {

  def ensureIndex(): Unit

}