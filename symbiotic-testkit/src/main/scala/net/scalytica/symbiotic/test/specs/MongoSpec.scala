package net.scalytica.symbiotic.test.specs

import java.net.{InetSocketAddress, Socket}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection}
import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.api.types.MetadataKeys._
import play.api.Configuration

/**
 * Use this trait when testing code that requires interaction with MongoDB.
 *
 * It works in the following way.
 *
 * 1. Check if there is a locally running MongoDB (with default port 27017) on the current system.
 * 2. a) If no local mongod, fail.
 * 2. b) A local DB is running, set the appropriate properties (including a specific test db name).
 * 3. let the specifications run through...
 * 4. Remove the test database from the locally running mongodb
 *
 */
trait MongoSpec extends PersistenceSpec {

  override val reposImpl = "net.scalytica.symbiotic.mongodb.MongoRepositories$"
  override val dbType    = "Mongo"
  override val dbPort    = 27017

  val localTestDBURI = s"mongodb://localhost:27017"

  override val configuration =
    Configuration(ConfigFactory.load()) ++ Configuration(
      "symbiotic.repository"          -> reposImpl,
      "symbiotic.mongodb.uri"         -> localTestDBURI,
      "symbiotic.mongodb.dbname.dman" -> dmanDBName
    )

  // scalastyle:off

  def initDatabase(): Either[String, Unit] = {
    val res = if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(dmanDBName).dropDatabase()
      Right(())
    } else {
      Left(
        s"[WARN] Preserving $dmanDBName DB as requested." +
          s" ¡¡¡IMPORTANT!!! DROP DB BEFORE NEW TEST RUN!"
      )
    }

    println(s"[INFO] Ensuring DB indices...")
    val db = MongoClient(MongoClientURI(localTestDBURI))(dmanDBName)
    index(new MongoCollection(db.getCollection("dman.files")))

    res
  }

  private def index(collection: MongoCollection): Unit = {
    val keysToindex = List(
      "filename"         -> false,
      OwnerKey.full      -> false,
      FidKey.full        -> false,
      UploadedByKey.full -> false,
      PathKey.full       -> false,
      VersionKey.full    -> false,
      IsFolderKey.full   -> false
    )

    println("Checking indices....")
    val background = MongoDBObject("background" -> true)
    val curr = collection.indexInfo
      .map(_.getAs[MongoDBObject]("key"))
      .filter(_.isDefined)
      .map(_.get.head._1)

    keysToindex.filterNot {
      case (k, _) => if (curr.nonEmpty) curr.contains(k) else false
    }.foreach {
      case (k, unique) =>
        println(
          s"Creating index for $k in collection ${collection.name}"
        )
        collection.createIndex(
          MongoDBObject(k                      -> 1),
          background ++ MongoDBObject("unique" -> unique)
        )
    }
  }

  // scalastyle:on

}
