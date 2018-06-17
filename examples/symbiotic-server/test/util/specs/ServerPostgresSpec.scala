package util.specs

import java.sql.DriverManager

import akka.actor.ActorSystem
import akka.stream.Materializer
import net.scalytica.symbiotic.config.ConfigReader
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

  private[this] val log = LoggerFactory.getLogger(classOf[ServerPostgresSpec])

  val cfgFile = "test-postgres.conf"

  sys.props += "config.resource" -> cfgFile

  override val configuration = Configuration(ConfigReader.load())

  override val dmanDBName =
    configuration.get[String]("symbiotic.persistence.postgres.schemaName")

  override val postgresDbName =
    configuration.get[String]("symbiotic.persistence.postgres.dbName")

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(configuration).build()

  implicit lazy val actorSys: ActorSystem      = app.actorSystem
  implicit lazy val materializer: Materializer = app.materializer

  override def afterAll(): Unit = {
    if (!isLocal) {
      throw new TestFailedException("No database available.", 0)
    }

    val c =
      DriverManager.getConnection(s"$baseUrl/$postgresDbName", dbUser, dbPass)
    val s = c.createStatement()
    try {
      val drops = Seq(
        s"DROP SCHEMA IF EXISTS $dmanDBName CASCADE",
        s"DROP TYPE public.gender_type",
        s"DROP TABLE public.play_evolutions"
      )
      log.debug(s"Executing:${drops.mkString("\n", "\n", "")}")
      drops.foreach { drop =>
        val d = s.executeUpdate(drop)
        log.debug(s"DROP statement returned $d")
      }
    } finally {
      s.close()
      c.close()
    }
    log.info("Removing temporary persistent file store at target/dman")
    new java.io.File("target/dman").delete()
  }
}
