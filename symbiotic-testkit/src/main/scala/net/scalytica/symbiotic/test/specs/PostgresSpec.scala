package net.scalytica.symbiotic.test.specs

import java.sql.{DriverManager, Statement}

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.io.Source
import scala.util.Try

trait PostgresSpec extends PersistenceSpec {

  private[this] val log = LoggerFactory.getLogger(classOf[PostgresSpec])

  override val dbHost =
    sys.props
      .get("CI")
      .orElse(sys.env.get("CI"))
      .map(_ => "postgres")
      .getOrElse("localhost")
  val postgresDbName  = "postgres"
  override val dbType = "Postgres"
  override val dbPort = 5432
  override val reposImpl =
    "net.scalytica.symbiotic.postgres.PostgresRepositories$"

  val dbUser: String = "postgres"
  val dbPass: String = dbUser
  val dbUrl: String =
    s"postgres://postgres:postgres@$dbHost:$dbPort/$postgresDbName"

  val sqlScript: String = Source
    .fromInputStream(
      getClass.getResourceAsStream("/sql/symbiotic-create-db.sql")
    )
    .getLines()
    .mkString

  // scalastyle:off
  val testConfig = Configuration(
    "symbiotic.repository"                                   -> reposImpl,
    "symbiotic.persistence.postgres.schemaName"              -> dmanDBName,
    "symbiotic.persistence.slick.dbs.dman.db.properties.url" -> dbUrl,
    "symbiotic.persistence.slick.dbs.dman.db.numThreads"     -> 2,
    "symbiotic.persistence.fs.rootDir"                       -> "target/dman/files",
    "akka.loggers"                                           -> Seq("akka.event.slf4j.Slf4jLogger"),
    "akka.loglevel"                                          -> "DEBUG",
    "akka.logging-filter"                                    -> "akka.event.slf4j.Slf4jLoggingFilter"
  )
  // scalastyle:on

  override val configuration: Configuration =
    Configuration(ConfigFactory.load()) ++ testConfig

  val baseUrl = s"jdbc:postgresql://$dbHost:$dbPort"

  override def initDatabase(): Either[String, Unit] = {
    if (!preserveDB) {
      val c = DriverManager.getConnection(s"$baseUrl/postgres", dbUser, dbPass)
      val s = c.createStatement()
      try {
        dbCleanAndInit(s)
      } finally {
        s.close()
        c.close()
      }
      log.info("Removing temporary persistent file store at target/dman")
      new java.io.File("target/dman").delete()
      Right(())
    } else {
      Left(
        s"Preserving $dmanDBName DB as requested." +
          s"¡¡¡IMPORTANT!!! DROP DB BEFORE NEW TEST RUN!"
      )
    }
  }

  protected def dbClean(s: Statement): Int = {
    s.executeUpdate(s"DROP SCHEMA IF EXISTS $dmanDBName CASCADE")
  }

  protected def dbCleanAndInit(s: Statement): Unit = {
    dbClean(s)
    // split ut the script into statements and execute them all
    sqlScript.split(";").map(_.trim).foreach { sql =>
      val testSql = sql.replaceAll("symbiotic_dman", dmanDBName)
      Try(s.executeUpdate(testSql)).map { _ =>
        log.debug(s"""statement "$testSql" executed""")
      }.getOrElse {
        log.warn(s"""statement "$testSql" failed""")
      }
    }
  }

}
