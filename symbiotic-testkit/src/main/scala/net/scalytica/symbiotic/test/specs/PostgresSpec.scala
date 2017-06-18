package net.scalytica.symbiotic.test.specs

import java.sql.DriverManager

import com.typesafe.config.ConfigFactory
import play.api.Configuration

import scala.io.Source

trait PostgresSpec extends PersistenceSpec {

  override val dbHost =
    sys.props
      .get("CI")
      .orElse(sys.env.get("CI"))
      .map(_ => "postgres")
      .getOrElse("localhost")
  override val dbType = "Postgres"
  override val dbPort = 5432
  override val reposImpl =
    "net.scalytica.symbiotic.postgres.PostgresRepositories$"

  val dbUser = "postgres"
  val dbPass = dbUser
  val dbUrl  = s"postgres://postgres:postgres@$dbHost:$dbPort/postgres"

  val sqlScript = Source
    .fromInputStream(
      getClass.getResourceAsStream("/sql/symbiotic-create-db.sql")
    )
    .getLines()
    .mkString

  // scalastyle:off
  override val configuration =
    Configuration(ConfigFactory.load()) ++ Configuration(
      "symbiotic.repository"              -> reposImpl,
      "symbiotic.postgres.schemaName"     -> dmanDBName,
      "symbiotic.slick.db.properties.url" -> dbUrl,
      "symbiotic.slick.db.numThreads"     -> 5,
      "symbiotic.fs.rootDir"              -> "target/dman/files"
    )

  override def initDatabase(): Either[String, Unit] = {
    val baseUrl = s"jdbc:postgresql://$dbHost:$dbPort"

    if (!preserveDB) {
      val c = DriverManager.getConnection(s"$baseUrl/postgres", dbUser, dbPass)
      val s = c.createStatement()
      try {
        val r1 = s.executeUpdate(s"DROP TABLE IF EXISTS $dmanDBName.files")
        val r2 = s.executeUpdate(s"DROP SCHEMA IF EXISTS $dmanDBName")
        // split ut the script into statements and execute them all
        sqlScript.split(";").map(_.trim).foreach { sql =>
          val testSql = sql.replaceAll("symbiotic_dman", dmanDBName)
          s.executeUpdate(testSql)
        }
      } finally {
        s.close()
        c.close()
      }
      println("[INFO] Removing temporary persistent file store at target/dman")
      new java.io.File("target/dman").delete()
      Right(())
    } else {
      Left(
        s"[WARN] Preserving $dmanDBName DB as requested. ¡¡¡IMPORTANT!!! " +
          s"DROP DB BEFORE NEW TEST RUN!"
      )
    }
  }

}
