package net.scalytica.symbiotic.test.specs

import java.net.{InetSocketAddress, Socket}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import org.slf4j.LoggerFactory
import play.api.Configuration

// scalastyle:off
trait PersistenceSpec
    extends WordSpecLike
    with MustMatchers
    with ScalaFutures
    with BeforeAndAfterAll {

  private val logger = LoggerFactory.getLogger(classOf[PersistenceSpec])

  val timeout: Double  = 10000d
  val interval: Double = 15d

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  val dmanDBName     = "test_symbiotic_dman"
  val dbHost: String = "localhost"
  val dbPort: Int
  val reposImpl: String
  val dbType: String

  lazy val isLocal = isLocalRunning

  val preserveDB = System.getProperty("db.preserve", "false").toBoolean

  def configuration: Configuration

  def config = configuration.underlying

  override def beforeAll(): Unit = {
    if (!isLocal) {
      throw new TestFailedException("No database available.", 0)
    }

    initDatabase() match {
      case Right(()) => logger.info("Database successfully initialized")
      case Left(err) => logger.warn(err)
    }
  }

  def initDatabase(): Either[String, Unit]

  def isLocalRunning: Boolean = {
    val address = new InetSocketAddress(dbHost, dbPort)
    val con     = new Socket
    try {
      con.connect(address)
      con.isConnected
    } catch {
      case e: Throwable =>
        logger.error(
          s"$dbType isn't running. Please start one up with " +
            s"the docker-${dbType.toLowerCase}.sh script."
        )
        false
    } finally {
      con.close()
    }
  }
}

// scalastyle:on
