/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package test.util.mongodb

import java.net.{InetSocketAddress, Socket, SocketAddress}

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import dman.FileWrapper
import org.specs2.specification.BeforeAfterSpec
import org.specs2.specification.core.Fragments
import org.specs2.specification.create.DefaultFragmentFactory

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
trait MongoSpec extends BeforeAfterSpec {

  val testDBName = "test_symbiotic"
  val hipeDBName = "test_symbiotic-hipe"
  val dmanDBName = "test_symbiotic-dman"
  val evtsDBName = "test_symbiotic-eventstore"

  val localTestDBURI = s"mongodb://localhost:27017"

  val isLocal = isLocalRunning

  val preserveDB = System.getProperty("db.preserve", "false").toBoolean

  System.setProperty("symbiotic.mongodb.uri", localTestDBURI)
  System.setProperty("symbiotic.mongodb.dbname.default", testDBName)
  System.setProperty("symbiotic.mongodb.dbname.hipe", hipeDBName)
  System.setProperty("symbiotic.mongodb.dbname.dman", dmanDBName)
  System.setProperty("akka.contrib.persistence.mongodb.mongo.db", evtsDBName)
  System.setProperty("akka.contrib.persistence.mongodb.mongo.journal-write-concern", "Acknowledged")

  cleanDatabase()

  override def beforeSpec: Fragments = Fragments(DefaultFragmentFactory.step {
    println("[INFO] ¡¡¡IMPORTANT!!! Tests might fail if test databases are not clean!")
  }, DefaultFragmentFactory.step {
    println(s"[INFO] Ensuring DB indices...")
    FileWrapper.ensureIndex()
  })

  override def afterSpec: Fragments = Fragments(DefaultFragmentFactory.step {
    cleanDatabase()
  })

  private def cleanDatabase(): Unit =
    if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(testDBName).dropDatabase()
      MongoClient(MongoClientURI(localTestDBURI))(hipeDBName).dropDatabase()
      MongoClient(MongoClientURI(localTestDBURI))(dmanDBName).dropDatabase()
      MongoClient(MongoClientURI(localTestDBURI))(evtsDBName).dropDatabase()
      println(s"[INFO] Dropped databases")
    } else {
      println("[WARN] Preserving databases as requested. ¡¡¡IMPORTANT!!! DROP DATABASE BEFORE NEW TEST RUN!")
    }

  /**
   * Tries to determine if there is a local mongod (with default port number) running on the current system.
   */
  private def isLocalRunning: Boolean = {
    val address: SocketAddress = new InetSocketAddress("localhost", 27017)
    val con = new Socket
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

}
