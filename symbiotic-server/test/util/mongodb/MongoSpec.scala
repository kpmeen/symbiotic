package util.mongodb

import java.net.{InetSocketAddress, Socket, SocketAddress}

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.mongodb.docmanagement.ManagedFilesIndex
import org.specs2.specification.BeforeAll
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
trait MongoSpec extends BeforeAll {

  val testDBName = "test_symbiotic"
  val dmanDBName = "test_symbiotic-dman"

  val localTestDBURI = s"mongodb://localhost:27017"

  val isLocal = isLocalRunning

  val preserveDB = System.getProperty("db.preserve", "false").toBoolean

  val configuration = Configuration(ConfigFactory.load()) ++ Configuration(
    "symbiotic.mongodb.uri"            -> localTestDBURI,
    "symbiotic.mongodb.dbname.default" -> testDBName,
    "symbiotic.mongodb.dbname.dman"    -> dmanDBName
  )

  lazy val config = configuration.underlying

  // scalastyle:off

  override def beforeAll = {
    println(
      "[INFO] ¡¡¡IMPORTANT!!! Tests might fail if test databases are not clean!"
    )
    cleanDatabase()
    println(s"[INFO] Ensuring DB indices...")
    new ManagedFilesIndex(config).ensureIndex()
  }

  private def cleanDatabase(): Unit =
    if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(testDBName).dropDatabase()
      MongoClient(MongoClientURI(localTestDBURI))(dmanDBName).dropDatabase()
      println(s"[INFO] Dropped databases")
    } else {
      println(
        "[WARN] Preserving DB as requested. ¡¡¡IMPORTANT!!! DROP DB BEFORE NEW TEST RUN!"
      )
    }

  /**
   * Tries to determine if there is a local mongod (with default port number) running on the current system.
   */
  def isLocalRunning: Boolean = {
    val address: SocketAddress = new InetSocketAddress("localhost", 27017)
    val con                    = new Socket
    try {
      con.connect(address)
      con.isConnected
    } catch {
      case e: Throwable =>
        println("[ERROR] Local MongoDB isn't running...")
        false
    } finally {
      con.close()
    }
  }

  // scalastyle:on

}
