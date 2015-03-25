/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.mongodb

import com.mongodb.casbah.gridfs.GridFS
import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection, MongoDB}
import com.mongodb.gridfs.{GridFS => MongoGridFS}
import com.typesafe.config.ConfigFactory
import play.api.Configuration

/**
 * Singleton keeping track of the MongoDB specifics around connectivity etc...
 */
object MongoContext {
  val defaultDBName: String = "symbiotic"

  val uri: MongoClientURI = {
    import play.api.Play.maybeApplication

    val conf = maybeApplication.map(_.configuration).getOrElse(Configuration(ConfigFactory.load()))
    val c = conf.getString("symbiotic.mongodb.uri").getOrElse(s"mongodb://localhost:27017/$defaultDBName")

    MongoClientURI(c)
  }

  def client: MongoClient = MongoClient(uri)
  def defaultDb: MongoDB = client(uri.database.getOrElse(defaultDBName))
}

/**
 * Trait providing access to a MongoClient, MongoDB and MongoCollection
 */
trait WithMongo {
  val collectionName: String

  def client = MongoContext.client

  def db = MongoContext.defaultDb

  lazy val collection: MongoCollection = db(collectionName)

}

/**
 * As WithMongo but additionally provides access to GridFS.
 */
trait WithGridFS extends WithMongo {

  val bucket: String = MongoGridFS.DEFAULT_BUCKET

  val collectionName: String = s"$bucket.files"

  lazy val gfs: GridFS = GridFS(db, bucket)

}

/**
 * Ensure index...
 */
trait WithMongoIndex {

  def ensureIndex(): Unit

}