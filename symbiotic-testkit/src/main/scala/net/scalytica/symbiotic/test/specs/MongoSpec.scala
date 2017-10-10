package net.scalytica.symbiotic.test.specs

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection}
import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.api.types.MetadataKeys._
import org.slf4j.LoggerFactory
import play.api.Configuration

/**
 * Use this trait when testing code that requires interaction with MongoDB.
 *
 * It works in the following way.
 *
 * 1. Check if there is a locally running MongoDB (with default port 27017)
 * on the current system.
 * 2. a) If no local mongod, fail.
 * 2. b) A local DB is running, set the appropriate properties
 * (including a specific test db name).
 * 3. let the specifications run through...
 * 4. Remove the test database from the locally running mongodb
 *
 */
trait MongoSpec extends PersistenceSpec {

  private val log = LoggerFactory.getLogger(classOf[MongoSpec])

  override val reposImpl = "net.scalytica.symbiotic.mongodb.MongoRepositories$"
  override val dbHost =
    sys.props
      .get("CI")
      .orElse(sys.env.get("CI"))
      .map(_ => "symbiotic-mongo")
      .getOrElse("localhost")
  override val dbType = "Mongo"
  override val dbPort = 27017

  val localTestDBURI = s"mongodb://$dbHost:$dbPort"

  // scalastyle:off
  override val configuration =
    Configuration(ConfigFactory.load()) ++ Configuration(
      "symbiotic.repository"                      -> reposImpl,
      "symbiotic.persistence.mongodb.uri"         -> localTestDBURI,
      "symbiotic.persistence.mongodb.dbname.dman" -> dmanDBName,
      "akka.loggers"                              -> Seq("akka.event.slf4j.Slf4jLogger"),
      "akka.loglevel"                             -> "DEBUG",
      "akka.logging-filter"                       -> "akka.event.slf4j.Slf4jLoggingFilter"
    )

  def clean(): Either[String, Unit] = {
    if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(dmanDBName).dropDatabase()
      Right(())
    } else {
      Left(
        s"Preserving $dmanDBName DB as requested." +
          s" ¡¡¡IMPORTANT!!! DROP DB BEFORE NEW TEST RUN!"
      )
    }
  }

  def initDatabase(): Either[String, Unit] = {
    val res = clean()

    log.info("Ensuring DB indices...")
    val db = MongoClient(MongoClientURI(localTestDBURI))(dmanDBName)
    index(new MongoCollection(db.getCollection("dman.files")))

    res
  }

  private def index(collection: MongoCollection): Unit = {
    val keysToindex = List(
      "filename"        -> false,
      OwnerKey.full     -> false,
      FidKey.full       -> false,
      CreatedByKey.full -> false,
      PathKey.full      -> false,
      VersionKey.full   -> false,
      IsFolderKey.full  -> false
    )

    log.info("Checking indices....")
    val background = MongoDBObject("background" -> true)
    val curr = collection.indexInfo
      .map(_.getAs[MongoDBObject]("key"))
      .filter(_.isDefined)
      .map(_.get.head._1)

    keysToindex.filterNot {
      case (k, _) => if (curr.nonEmpty) curr.contains(k) else false
    }.foreach {
      case (k, unique) =>
        log.info(s"Creating index for $k in collection ${collection.name}")
        collection.createIndex(
          MongoDBObject(k                      -> 1),
          background ++ MongoDBObject("unique" -> unique)
        )
    }
  }

  // scalastyle:on

}
