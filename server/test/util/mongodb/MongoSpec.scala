/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package util.mongodb

import java.net.{InetSocketAddress, Socket, SocketAddress}

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.typesafe.config.ConfigFactory
import org.specs2.specification.BeforeAll
import play.api.Configuration
import repository.mongodb.docmanagement.ManagedFilesIndex

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

  val config = Configuration(ConfigFactory.load()) ++ Configuration(
    "symbiotic.mongodb.uri"            -> localTestDBURI,
    "symbiotic.mongodb.dbname.default" -> testDBName,
    "symbiotic.mongodb.dbname.dman"    -> dmanDBName
  )

  override def beforeAll = {
    println(
      "[INFO] ¡¡¡IMPORTANT!!! Tests might fail if test databases are not clean!"
    ) // scalastyle:ignore
    cleanDatabase()
    println(s"[INFO] Ensuring DB indices...") // scalastyle:ignore
    new ManagedFilesIndex(config).ensureIndex()
  }

  private def cleanDatabase(): Unit =
    if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(testDBName).dropDatabase()
      MongoClient(MongoClientURI(localTestDBURI))(dmanDBName).dropDatabase()
      println(s"[INFO] Dropped databases") // scalastyle:ignore
    } else {
      println(
        "[WARN] Preserving DB as requested. ¡¡¡IMPORTANT!!! DROP DB BEFORE NEW TEST RUN!"
      ) // scalastyle:ignore
    }

  /**
   * Tries to determine if there is a local mongod (with default port number) running on the current system.
   */
  def isLocalRunning: Boolean = {
    val address: SocketAddress = new InetSocketAddress("localhost", 27017) // scalastyle:ignore
    val con                    = new Socket
    try {
      con.connect(address)
      con.isConnected
    } catch {
      case e: Throwable =>
        println("[ERROR] Local MongoDB isn't running...") // scalastyle:ignore
        false
    } finally {
      con.close()
    }
  }

}
