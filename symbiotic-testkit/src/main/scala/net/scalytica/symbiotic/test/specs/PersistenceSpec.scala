package net.scalytica.symbiotic.test.specs

import java.net.{InetSocketAddress, Socket}

import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Millis, Span}
import play.api.Configuration

// scalastyle:off
trait PersistenceSpec
    extends WordSpecLike
    with MustMatchers
    with ScalaFutures
    with BeforeAndAfterAll {

  val timeout: Double  = 10000d
  val interval: Double = 15d

  val dmanDBName     = "test_symbiotic_dman"
  val dbHost: String = "localhost"
  val dbPort: Int
  val reposImpl: String
  val dbType: String

  lazy val isLocal = isLocalRunning

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  val preserveDB = System.getProperty("db.preserve", "false").toBoolean

  val configuration: Configuration

  def config = configuration.underlying

  override def beforeAll: Unit = {
    if (!isLocal) {
      throw new TestFailedException("[ERROR] No local Postgres available.", 0)
    }

    initDatabase() match {
      case Right(()) => println("[INFO] Database successfully initialized")
      case Left(err) => println(err)
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
        println(
          s"[ERROR] Local $dbType isn't running. Please start one up with " +
            s"the docker-${dbType.toLowerCase}.sh script."
        )
        false
    } finally {
      con.close()
    }
  }
}
// scalastyle:on
