/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package util.mongodb

import com.mongodb.util.JSON
import com.mongodb.{BasicDBObject, DB, Mongo, MongoClient}
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net, RuntimeConfigBuilder, Storage}
import de.flapdoodle.embed.mongo.{Command, MongodProcess, MongodStarter}
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.io.NullProcessor
import de.flapdoodle.embed.process.runtime.Network
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * Allows starting a MongoDB instance in a test case scenario
 *
 * @param mongoPort The port to use. If not specified a random free server port will be used.
 */
class MongoRunner(mongoPort: Int = Network.getFreeServerPort) {

  val logger = LoggerFactory.getLogger(this.getClass)

  // TODO: Why isn't v3.0.x properly shutting down?
  private val mongoVersion = MongoVersion.V2_6_8 //MongoVersion.V3_0_0

  val port = mongoPort

  val MongoHost = Network.getLocalHost.getHostAddress

  val replSetName = s"repl$MongoHost"

  private var mongodProcess: MongodProcess = null

  private lazy val MongodExecutable = {
    logger.info("Configuring MongoExecutable...")
    val config = new MongodConfigBuilder()
      .version(mongoVersion)
      .net(new Net(port, Network.localhostIsIPv6()))
      .replication(new Storage(null, replSetName, 1024))
      .build
    val streamProc = new NullProcessor
    val runtimeConfig = new RuntimeConfigBuilder()
      .defaults(Command.MongoD)
      .processOutput(new ProcessOutput(streamProc, streamProc, streamProc))
      .build

    logger.info(s"MongoExecutable configured to run on $MongoHost port $port")
    MongodStarter.getInstance(runtimeConfig).prepare(config)
  }

  /**
   * @return true if the process is running, otherwise false
   */
  def running: Boolean = mongodProcess != null && mongodProcess.isProcessRunning

  /**
   * @return Returns a new running mongod process
   */
  def startMongod(): Unit = synchronized {
    logger.info(s"Starting embedded MongoDB instance on $port...")

    def isReplicaSetStarted(adb: DB): Boolean = {
      val status = adb.command(new BasicDBObject("replSetGetStatus", 1))
      val currStatus = Try(status.getInt("myState")).recover { case e: Throwable => -1 }.get
      if (currStatus != 1) {
        false
      } else {
        true
      }
    }

    mongodProcess = MongodExecutable.start()
    while (!mongodProcess.isProcessRunning) {}

    // Initialize MongoDB as a replicaSet
    val adminDB = createClient().getDB("admin")
    val cr = adminDB.command(new BasicDBObject("isMaster", 1))
    logger.debug(s"isMaster: $cr")
    val rs = adminDB.command(new BasicDBObject("replSetInitiate", JSON.parse( s"""{"_id": "$replSetName", "members": [{"_id": 0, "host": "$MongoHost:$port"}]}""")))
    logger.debug(s"replSetInitiate result: $rs")

    while (!isReplicaSetStarted(adminDB)) {}

    logger.info(s"Embedded MongoDB started with PID ${mongodProcess.getProcessId}...ready to accept connections on $port")
  }

  /**
   * Tries to stop, halt/kill the running mongod process.
   */
  def stopMongod(): Unit = synchronized {
    logger.info(s"Going to stop embedded MongoDB instance running on $port with PID ${mongodProcess.getProcessId}")
    try {
      mongodProcess.stop()
      while (mongodProcess.isProcessRunning) {
        logger.info(s"MongoDB instance on $port with PID ${mongodProcess.getProcessId} is still running...")
      }
      logger.info(s"Embedded MongoDB instance stopped...")
    } catch {
      case e: Exception =>
        logger.error(s"Problem stopping embedded mongod process with PID: ${mongodProcess.getProcessId}", e)
        logger.info("Trying again...")
        mongodProcess.stop()
    } finally {
      MongodExecutable.stop()
    }
  }

  def createClient(): Mongo = new MongoClient(MongoHost, port)

}