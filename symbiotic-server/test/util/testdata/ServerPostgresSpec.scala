package util.testdata

import java.sql.DriverManager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.test.specs.PostgresSpec
import org.scalatest.exceptions.TestFailedException
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.slf4j.LoggerFactory
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}

trait ServerPostgresSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with PostgresSpec {

  private val log = LoggerFactory.getLogger(classOf[ServerPostgresSpec])

  override val configuration = Configuration(
    ConfigFactory.load("test-postgres.conf").resolve()
  )

  implicit val actorSys: ActorSystem           = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(configuration).build()

  override implicit lazy val app: Application = fakeApplication()

  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = {
    if (!isLocal) {
      throw new TestFailedException("No database available.", 0)
    }

    val c = DriverManager.getConnection(s"$baseUrl/postgres", dbUser, dbPass)
    val s = c.createStatement()
    try {
      val res = dbClean(s)
      s.executeUpdate("DROP TABLE public.play_evolutions")
      s.executeUpdate("DROP TYPE public.gender_type")

      log.info(s"Removed tables and schema for $dmanDBName returned $res")
    } finally {
      s.close()
      c.close()
    }
    log.info("Removing temporary persistent file store at target/dman")
    new java.io.File("target/dman").delete()
  }
}
